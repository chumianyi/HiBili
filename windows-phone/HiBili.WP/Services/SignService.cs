// Hi!Bili - request signing (AppKey MD5 + WBI w_rid).
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using HiBili.WP.Helpers;

namespace HiBili.WP.Services
{
    /// <summary>
    /// Implements the two signature schemes used by Bilibili's open API:
    ///
    ///   1. Legacy AppKey sign (tv-login, old open endpoints):
    ///        sort params by key, append "appkey=&lt;appkey&gt;&lt;appsec&gt;",
    ///        md5 the whole query string -> &sign=...
    ///
    ///   2. WBI sign (web endpoints after 2023):
    ///        pull img_key/sub_key from /x/web-interface/nav,
    ///        shuffle them through the public 64-index table to form a mixin key,
    ///        append &w_rid=md5(sorted_query + mixin_key).
    /// </summary>
    public static class SignService
    {
        // Fixed credentials shipped with the reference build.
        public const string AppKey = "dfca71928277209b";
        public const string AppSec = "b5475a8825547a4fc26c7d518eaaa02e";

        /// <summary>
        /// The public WBI permutation table (64 indices into the 64-char mixed key).
        /// Taken verbatim from Bilibili's own web bundle; do not "fix" it.
        /// </summary>
        private static readonly int[] WbiMixTable =
        {
            46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,
            27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,
            37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,
            22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52
        };

        /// <summary>Cached mixin key, refreshed by <see cref="RefreshWbiKeys"/>.</summary>
        private static string _mixinKey;

        /// <summary>
        /// Build the legacy AppKey signed query string (without the leading '?').
        /// </summary>
        public static string BuildSignedQuery(IDictionary<string, string> parameters)
        {
            var dict = new Dictionary<string, string>(parameters ?? new Dictionary<string, string>());
            dict["appkey"] = AppKey;

            // Sort by key, URL-encode value.
            var ordered = dict.OrderBy(kv => kv.Key, StringComparer.Ordinal);
            var sb = new StringBuilder();
            bool first = true;
            foreach (var kv in ordered)
            {
                if (!first) sb.Append('&');
                sb.Append(WebUtility.UrlEncode(kv.Key));
                sb.Append('=');
                sb.Append(WebUtility.UrlEncode(kv.Value ?? string.Empty));
                first = false;
            }

            // Append appsec, md5.
            sb.Append(AppSec);
            string sign = Md5Helper.HexAscii(sb.ToString());

            // Rebuild without appsec appended, add sign.
            var query = new StringBuilder();
            first = true;
            foreach (var kv in ordered)
            {
                if (!first) query.Append('&');
                query.Append(WebUtility.UrlEncode(kv.Key));
                query.Append('=');
                query.Append(WebUtility.UrlEncode(kv.Value ?? string.Empty));
                first = false;
            }
            query.Append("&sign=").Append(sign);
            return query.ToString();
        }

        /// <summary>
        /// Compute the WBI mixin key from the raw img_url / sub_url key halves.
        /// </summary>
        public static string ComputeMixinKey(string imgKey, string subKey)
        {
            string raw = (imgKey ?? string.Empty) + (subKey ?? string.Empty);
            var sb = new StringBuilder();
            for (int i = 0; i < 32; i++)
            {
                int idx = WbiMixTable[i];
                if (idx < raw.Length) sb.Append(raw[idx]);
            }
            return sb.ToString();
        }

        /// <summary>
        /// Cache the mixin key extracted from /x/web-interface/nav.
        /// </summary>
        public static void RefreshWbiKeys(string imgUrl, string subUrl)
        {
            string imgKey = ExtractKeyFromUrl(imgUrl);
            string subKey = ExtractKeyFromUrl(subUrl);
            _mixinKey = ComputeMixinKey(imgKey, subKey);
        }

        /// <summary>
        /// Apply WBI signing in-place: adds wts and w_rid to <paramref name="parameters"/>.
        /// Call <see cref="RefreshWbiKeys"/> once after boot before using this.
        /// </summary>
        public static void ApplyWbiSign(IDictionary<string, string> parameters)
        {
            if (string.IsNullOrEmpty(_mixinKey))
            {
                // Fallback: a well-known static mixin key lets prototype builds run
                // against endpoints that still accept unsigned traffic.
                _mixinKey = ComputeMixinKey(
                    "7cd0841336825a4048aa82375d319e3a",
                    "dd5d78462967de919fea8b1fdffe775b");
            }

            parameters["wts"] = DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString();

            // Drop unexpected chars per Bilibili spec (!'()*), then sort.
            var ordered = parameters
                .OrderBy(kv => kv.Key, StringComparer.Ordinal)
                .ToDictionary(kv => kv.Key, kv => SanitizeWbiValue(kv.Value));

            var sb = new StringBuilder();
            bool first = true;
            foreach (var kv in ordered)
            {
                if (!first) sb.Append('&');
                sb.Append(WebUtility.UrlEncode(kv.Key));
                sb.Append('=');
                sb.Append(WebUtility.UrlEncode(kv.Value ?? string.Empty));
                first = false;
            }
            sb.Append(_mixinKey);

            parameters["w_rid"] = Md5Helper.HexUtf8(sb.ToString());
        }

        private static string ExtractKeyFromUrl(string url)
        {
            if (string.IsNullOrEmpty(url)) return string.Empty;
            int slash = url.LastIndexOf('/');
            if (slash < 0 || slash >= url.Length - 1) return string.Empty;
            string file = url.Substring(slash + 1);
            int dot = file.IndexOf('.');
            return dot > 0 ? file.Substring(0, dot) : file;
        }

        private static string SanitizeWbiValue(string value)
        {
            if (string.IsNullOrEmpty(value)) return value;
            // Bilibili strips these characters from WBI values.
            foreach (char c in new[] { '!', '\'', '(', ')', '*' })
            {
                value = value.Replace(c.ToString(), string.Empty);
            }
            return value;
        }
    }
}
