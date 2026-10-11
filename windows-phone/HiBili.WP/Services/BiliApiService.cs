// Hi!Bili - Bilibili HTTP API client.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Threading.Tasks;
using HiBili.WP.Helpers;
using HiBili.WP.Models;

namespace HiBili.WP.Services
{
    /// <summary>
    /// Single HTTP façade for the Bilibili endpoints the reference client touches.
    ///
    /// This is intentionally small: the only *working* flows are
    ///   - TV QR-code login (auth_code -> poll -> access_token)
    ///   - profile fetch (/x/web-interface/nav)
    ///   - logout (/login/exit/v2)
    ///
    /// Content-creation endpoints are declared as stub extension points so the
    /// UI layer can call them once the upload pipeline is ported. They are
    /// marked with the TODO comment required by the project spec.
    /// </summary>
    public class BiliApiService
    {
        private const string TvBase = "https://passport.bilibili.tv";
        private const string WebBase = "https://passport.bilibili.com";
        private const string ApiBase = "https://api.bilibili.com";

        private static readonly Lazy<BiliApiService> _instance =
            new Lazy<BiliApiService>(() => new BiliApiService());

        public static BiliApiService Instance
        {
            get { return _instance.Value; }
        }

        private readonly HttpClient _http;

        public BiliApiService()
        {
            _http = new HttpClient();
            _http.DefaultRequestHeaders.UserAgent.ParseAdd(
                "Mozilla/5.0 (Windows Phone 8.1; HiBili/1.0)");
            _http.DefaultRequestHeaders.Accept.Add(
                new MediaTypeWithQualityHeaderValue("application/json"));
        }

        // --------------------------------------------------------------------
        // TV QR-code login
        // --------------------------------------------------------------------

        /// <summary>
        /// Step 1: ask the TV-login endpoint for an auth_code + QR payload URL.
        /// </summary>
        public async Task<ApiResponse<QrCodeAuthData>> GetTvAuthCodeAsync()
        {
            var form = new Dictionary<string, string>
            {
                { "local_id", "0" },
                { "ts", DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString() }
            };
            string query = SignService.BuildSignedQuery(form);
            string url = TvBase + "/x/passport-tv-login/qrcode/auth_code?" + query;

            // POST with empty body is what the TV endpoint expects.
            var resp = await _http.PostAsync(url, new StringContent(string.Empty, Encoding.UTF8, "application/x-www-form-urlencoded"));
            return await DeserializeAsync<ApiResponse<QrCodeAuthData>>(resp);
        }

        /// <summary>
        /// Step 2: poll for the scanned/confirmed state.
        /// Common codes:
        ///   0     -> success, <see cref="LoginResult"/> is in Data
        ///   86038 -> QR not scanned yet
        ///   86090 -> scanned, waiting for confirm on phone
        ///   86091 -> QR expired
        /// </summary>
        public async Task<ApiResponse<LoginResult>> PollTvAuthCodeAsync(string authCode)
        {
            var form = new Dictionary<string, string>
            {
                { "auth_code", authCode ?? string.Empty },
                { "ts", DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString() }
            };
            string query = SignService.BuildSignedQuery(form);
            string url = TvBase + "/x/passport-tv-login/qrcode/poll?" + query;

            var resp = await _http.PostAsync(url, new StringContent(string.Empty, Encoding.UTF8, "application/x-www-form-urlencoded"));
            return await DeserializeAsync<ApiResponse<LoginResult>>(resp);
        }

        // --------------------------------------------------------------------
        // Profile
        // --------------------------------------------------------------------

        /// <summary>
        /// Fetch /x/web-interface/nav to refresh WBI keys and the user profile.
        /// Also pushes the img_url / sub_url into <see cref="SignService"/>.
        /// </summary>
        public async Task<UserInfo> GetNavInfoAsync()
        {
            string url = ApiBase + "/x/web-interface/nav";
            using (var req = BuildSignedGet(url))
            using (var resp = await _http.SendAsync(req))
            {
                // The nav endpoint ships WBI keys nested inside wbi_img.
                // Deserialize loosely: pull the whole stream and read fields we need.
                string json = await resp.Content.ReadAsStringAsync();
                try
                {
                    using (var ms = new MemoryStream(Encoding.UTF8.GetBytes(json)))
                    {
                        var ser = new DataContractJsonSerializer(typeof(NavEnvelope));
                        var env = (NavEnvelope)ser.ReadObject(ms);
                        if (env == null || env.data == null) return null;

                        if (env.data.wbi_img != null)
                        {
                            SignService.RefreshWbiKeys(env.data.wbi_img.img_url, env.data.wbi_img.sub_url);
                        }

                        return new UserInfo
                        {
                            IsLogin = env.data.IsLogin,
                            Mid = env.data.Mid,
                            Name = env.data.Name,
                            Face = env.data.Face,
                            VipStatus = env.data.VipStatus,
                            VipDueDate = env.data.VipDueDate
                        };
                    }
                }
                catch
                {
                    return null;
                }
            }
        }

        // --------------------------------------------------------------------
        // Legacy password login (kept for the Pivot tab; not actively used)
        // --------------------------------------------------------------------

        /// <summary>
        /// Exchange username + password for a session. This path goes through the
        /// /x/passport-login/web/key -> /x/passport-login/oauth2/login flow.
        /// It is provided so the "密码登录" tab has a real call wired up, but
        /// TV QR login is the recommended path on this platform.
        /// </summary>
        public async Task<ApiResponse<LoginResult>> LoginByPasswordAsync(string username, string password)
        {
            // 1) fetch key (hash + RSA modulus/exponent)
            var keyResp = await _http.GetAsync(WebBase + "/x/passport-login/web/key");
            var key = await DeserializeAsync<ApiResponse<WebKeyData>>(keyResp);
            if (key == null || !key.IsSuccess || key.Data == null)
            {
                return new ApiResponse<LoginResult>
                {
                    Code = key != null ? key.Code : -1,
                    Message = "无法获取加密公钥"
                };
            }

            // 2) RSA-encrypt hash + password
            string encrypted;
            try
            {
                byte[] modulus = Convert.FromBase64String(key.Data.Modulus);
                byte[] exponent = Convert.FromBase64String(key.Data.Exponent);
                encrypted = RsaHelper.EncryptToBase64(modulus, exponent, key.Data.Hash + password);
            }
            catch (Exception ex)
            {
                return new ApiResponse<LoginResult> { Code = -2, Message = "密码加密失败: " + ex.Message };
            }

            // 3) post login
            var form = new Dictionary<string, string>
            {
                { "username", username ?? string.Empty },
                { "password", encrypted },
                { "ts", DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString() }
            };
            string query = SignService.BuildSignedQuery(form);
            var resp = await _http.PostAsync(
                WebBase + "/x/passport-login/oauth2/login?" + query,
                new StringContent(string.Empty, Encoding.UTF8, "application/x-www-form-urlencoded"));
            return await DeserializeAsync<ApiResponse<LoginResult>>(resp);
        }

        // --------------------------------------------------------------------
        // Logout
        // --------------------------------------------------------------------

        /// <summary>
        /// POST /login/exit/v2 with the current cookie; on success the local
        /// token store is cleared by the caller.
        /// </summary>
        public async Task<ApiResponse> ExitLoginAsync()
        {
            var form = new Dictionary<string, string>
            {
                { "bili_csrf", ExtractBiliCsrf() },
                { "ts", DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString() }
            };
            string query = SignService.BuildSignedQuery(form);
            var req = new HttpRequestMessage(HttpMethod.Post, WebBase + "/login/exit/v2?" + query);
            AttachCookies(req);
            var resp = await _http.SendAsync(req);
            return await DeserializeAsync<ApiResponse>(resp);
        }

        // --------------------------------------------------------------------
        // TODO: 发布接口扩展点，UI 暂未接入
        // （HTTP 调用已完整实现，仅未在 XAML 页面上挂发布入口）
        // --------------------------------------------------------------------

        /// <summary>
        /// TODO: 发布接口扩展点，UI 暂未接入
        /// Post a text-only dynamic to /x/vc/dynamic/create.
        /// The request carries WBI sign (wts/w_rid) and the bili_jct CSRF token.
        /// </summary>
        public async Task<ApiResponse> CreateDynamicAsync(string text, string atUids = null, string atTopics = null)
        {
            // dyn_req is a JSON blob the server parses server-side.
            var atList = string.IsNullOrEmpty(atUids)
                ? new long[0]
                : Array.ConvertAll(atUids.Split(','), s => long.Parse(s.Trim()));
            var sb = new StringBuilder();
            sb.Append("{\"text\":\"").Append(EscapeJson(text ?? string.Empty)).Append("\"");
            if (atList.Length > 0)
            {
                sb.Append(",\"at_uids\":[");
                for (int i = 0; i < atList.Length; i++)
                {
                    if (i > 0) sb.Append(',');
                    sb.Append(atList[i]);
                }
                sb.Append(']');
            }
            sb.Append(",\"ctrl\":[]}");

            var form = new Dictionary<string, string>
            {
                { "dynamic_id", "0" },
                { "dyn_req", sb.ToString() },
                { "up_choose_layout", "0" },
                { "publish_time", "0" },
                { "bili_csrf", ExtractBiliCsrf() }
            };

            return await PostFormSignedAsync(ApiBase + "/x/vc/dynamic/create", form);
        }

        /// <summary>
        /// TODO: 发布接口扩展点，UI 暂未接入
        /// Post a comment/reply to /x/v2/reply/add with WBI sign + bili_jct CSRF.
        /// </summary>
        public async Task<ApiResponse> ReplyAddAsync(long oid, int type, string message, long root = 0, long parent = 0)
        {
            var form = new Dictionary<string, string>
            {
                { "oid", oid.ToString() },
                { "type", type.ToString() },
                { "message", message ?? string.Empty },
                { "root", root.ToString() },
                { "parent", parent.ToString() },
                { "plat", "1" },
                { "bili_csrf", ExtractBiliCsrf() }
            };
            return await PostFormSignedAsync(ApiBase + "/x/v2/reply/add", form);
        }

        /// <summary>
        /// TODO: 发布接口扩展点，UI 暂未接入
        /// Upload an image to the bilibili picture bed via multipart/form-data.
        /// </summary>
        public async Task<ApiResponse> UploadImageAsync(byte[] pngBytes, string biz = "dyn")
        {
            if (pngBytes == null || pngBytes.Length == 0)
            {
                return new ApiResponse { Code = -1, Message = "图片数据为空" };
            }

            var form = new Dictionary<string, string>
            {
                { "biz", biz ?? "dyn" },
                { "csrf", ExtractBiliCsrf() }
            };
            SignService.ApplyWbiSign(form);
            string query = BuildQuery(form);

            using (var content = new MultipartFormDataContent())
            {
                // The image bytes part.
                var fileContent = new ByteArrayContent(pngBytes);
                fileContent.Headers.ContentType = new MediaTypeHeaderValue("image/png");
                fileContent.Headers.ContentDisposition =
                    new ContentDispositionHeaderValue("form-data")
                    {
                        Name = "file",
                        FileName = "upload.png"
                    };
                content.Add(fileContent);

                // Non-file fields.
                content.Add(new StringContent(biz ?? "dyn", Encoding.UTF8), "biz");
                content.Add(new StringContent(ExtractBiliCsrf(), Encoding.UTF8), "csrf");

                var req = new HttpRequestMessage(HttpMethod.Post, ApiBase + "/x/vc/dynamic/upload-img?" + query)
                {
                    Content = content
                };
                AttachCookies(req);
                var resp = await _http.SendAsync(req);
                return await DeserializeAsync<ApiResponse>(resp);
            }
        }

        /// <summary>
        /// POST url-encoded form with WBI sign appended to the URL.
        /// </summary>
        private async Task<ApiResponse> PostFormSignedAsync(string baseUrl, IDictionary<string, string> form)
        {
            SignService.ApplyWbiSign(form);
            string body = BuildQuery(form);

            var req = new HttpRequestMessage(HttpMethod.Post, baseUrl)
            {
                Content = new StringContent(body, Encoding.UTF8, "application/x-www-form-urlencoded")
            };
            AttachCookies(req);
            var resp = await _http.SendAsync(req);
            return await DeserializeAsync<ApiResponse>(resp);
        }

        private static string BuildQuery(IDictionary<string, string> form)
        {
            var sb = new StringBuilder();
            bool first = true;
            foreach (var kv in form)
            {
                if (!first) sb.Append('&');
                sb.Append(WebUtility.UrlEncode(kv.Key));
                sb.Append('=');
                sb.Append(WebUtility.UrlEncode(kv.Value ?? string.Empty));
                first = false;
            }
            return sb.ToString();
        }

        private static string EscapeJson(string s)
        {
            return s.Replace("\\", "\\\\").Replace("\"", "\\\"").Replace("\n", "\\n");
        }

        // --------------------------------------------------------------------
        // Internals
        // --------------------------------------------------------------------

        private HttpRequestMessage BuildSignedGet(string url)
        {
            var req = new HttpRequestMessage(HttpMethod.Get, url);
            AttachCookies(req);
            return req;
        }

        private void AttachCookies(HttpRequestMessage req)
        {
            string cookie = AccountService.Instance.Cookie;
            if (!string.IsNullOrEmpty(cookie))
            {
                req.Headers.Add("Cookie", cookie);
            }
            string access = AccountService.Instance.AccessToken;
            if (!string.IsNullOrEmpty(access))
            {
                req.Headers.Add("Authorization", "Bearer " + access);
            }
        }

        private string ExtractBiliCsrf()
        {
            string cookie = AccountService.Instance.Cookie;
            if (string.IsNullOrEmpty(cookie)) return string.Empty;
            foreach (var pair in cookie.Split(';'))
            {
                var kv = pair.Split('=');
                if (kv.Length == 2 && kv[0].Trim() == "bili_jct")
                {
                    return kv[1].Trim();
                }
            }
            return string.Empty;
        }

        private static async Task<T> DeserializeAsync<T>(HttpResponseMessage resp)
        {
            string json = await resp.Content.ReadAsStringAsync();
            if (string.IsNullOrEmpty(json)) return default(T);
            using (var ms = new MemoryStream(Encoding.UTF8.GetBytes(json)))
            {
                var ser = new DataContractJsonSerializer(typeof(T));
                return (T)ser.ReadObject(ms);
            }
        }

        // Lightweight envelopes only used internally for WBI key extraction.
        [DataContract]
        private class NavEnvelope
        {
            [DataMember(Name = "code")] public int code;
            [DataMember(Name = "data")] public NavData data;
        }

        [DataContract]
        private class NavData
        {
            [DataMember(Name = "isLogin")] public bool IsLogin;
            [DataMember(Name = "mid")] public long Mid;
            [DataMember(Name = "uname")] public string Name;
            [DataMember(Name = "face")] public string Face;
            [DataMember(Name = "vipStatus")] public int VipStatus;
            [DataMember(Name = "vipDueDate")] public long VipDueDate;
            [DataMember(Name = "wbi_img")] public WbiImg wbi_img;
        }

        [DataContract]
        private class WbiImg
        {
            [DataMember(Name = "img_url")] public string img_url;
            [DataMember(Name = "sub_url")] public string sub_url;
        }

        [DataContract]
        private class WebKeyData
        {
            [DataMember(Name = "hash")] public string Hash;
            [DataMember(Name = "modulus")] public string Modulus;
            [DataMember(Name = "exponent")] public string Exponent;
        }
    }
}
