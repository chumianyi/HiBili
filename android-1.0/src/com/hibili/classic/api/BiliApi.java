/*
 * This file is part of Hi！Bili.
 *
 * Hi！Bili is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hi！Bili is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hi！Bili.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hibili.classic.api;

import com.hibili.classic.account.AccountManager;
import com.hibili.classic.util.HttpUtil;
import com.hibili.classic.util.RsaUtil;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * B 站接口封装（参考版本，简化实现）。
 *
 * 所有方法均为同步阻塞调用，必须在子线程中执行。
 * 网络层使用 HttpURLConnection，JSON 使用 org.json。
 *
 * 已知限制：Android 1.0 时代的 TLS 栈无法协商现代 HTTPS，
 * 这些接口在真实 Android 1.0 设备上大概率因握手失败而不可用；
 * 本类用于展示接口流程与签名逻辑。
 */
public final class BiliApi {

    private static final String HOST_API = "https://api.bilibili.com";
    private static final String HOST_PASSPORT = "https://passport.bilibili.com";

    private final AccountManager account;

    public BiliApi(AccountManager account) {
        this.account = account;
    }

    // ------------------------------------------------------------------
    // TV 扫码登录
    // ------------------------------------------------------------------

    /**
     * 请求 TV 扫码授权码。
     *
     * @return JSONObject：{ "auth_code":..., "qrcode_img_url":..., "url":... }；失败 null
     */
    public JSONObject requestTvAuthCode() {
        TreeMap params = new TreeMap();
        params.put("local_id", "0");
        params.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        TreeMap signed = SignUtil.signWithAppkey(params);

        String resp = HttpUtil.postForm(
                HOST_PASSPORT + "/x/passport-tv-login/qrcode/auth_code",
                signed, null);
        JSONObject json = parse(resp);
        if (json == null || json.optInt("code", -1) != 0) {
            return null;
        }
        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            return null;
        }
        String authCode = data.optString("auth_code", "");
        String url = data.optString("url", "");

        JSONObject out = new JSONObject();
        try {
            out.put("auth_code", authCode);
            out.put("url", url);
            // 用公共二维码图片服务把登录 URL 渲染成图片，交给 WebView 显示
            out.put("qrcode_img_url",
                    "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data="
                            + java.net.URLEncoder.encode(url, "UTF-8"));
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    /**
     * 轮询扫码状态。
     *
     * @return code：0=成功(返回 data 含 token) / 86038=等待扫码 /
     *         86039=已扫码待确认 / -1=失败；以及解析后的登录数据
     */
    public JSONObject pollTvAuthCode(String authCode) {
        TreeMap params = new TreeMap();
        params.put("auth_code", authCode);
        params.put("local_id", "0");
        params.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        TreeMap signed = SignUtil.signWithAppkey(params);

        String resp = HttpUtil.postForm(
                HOST_PASSPORT + "/x/passport-tv-login/qrcode/poll",
                signed, null);
        return parse(resp);
    }

    // ------------------------------------------------------------------
    // 密码登录
    // ------------------------------------------------------------------

    /** 获取登录加密公钥与 hash key。返回 { hash, key }；失败 null。 */
    public JSONObject getLoginKey() {
        String resp = HttpUtil.get(HOST_API + "/x/passport-login/web/key", null);
        JSONObject json = parse(resp);
        if (json == null || json.optInt("code", -1) != 0) {
            return null;
        }
        return json.optJSONObject("data");
    }

    /**
     * 密码登录。
     *
     * @param username 手机号/邮箱
     * @param password 明文密码
     * @return 原始响应 JSONObject；失败 null
     */
    public JSONObject passwordLogin(String username, String password) {
        JSONObject keyData = getLoginKey();
        if (keyData == null) {
            return null;
        }
        String hash = keyData.optString("hash", "");
        String pubKey = keyData.optString("key", "");

        // B 站约定：加密内容 = hash + 明文密码
        String rsaInput = hash + password;
        String encryptedPwd = RsaUtil.encrypt(pubKey, rsaInput);

        TreeMap params = new TreeMap();
        params.put("username", username);
        params.put("password", encryptedPwd);
        params.put("keep", "0");
        params.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        TreeMap signed = SignUtil.signWithAppkey(params);

        String resp = HttpUtil.postForm(
                HOST_PASSPORT + "/x/passport-login/oauth2/login",
                signed, null);
        return parse(resp);
    }

    // ------------------------------------------------------------------
    // 发布 / 评论
    // ------------------------------------------------------------------

    /** 发布纯文字动态（scene=1）。返回响应 JSONObject；失败 null。 */
    public JSONObject createTextDynamic(String text) {
        if (!account.isLoggedIn()) {
            return null;
        }
        TreeMap params = new TreeMap();
        params.put("dyn_req", "1");
        params.put("at_uids", "");
        params.put("ctrl", "[]");
        params.put("description", text == null ? "" : text);
        params.put("scene", "1");
        params.put("upload_id", String.valueOf(System.currentTimeMillis()));
        params.put("access_key", account.getAccessToken());
        TreeMap signed = SignUtil.signWithAppkey(params);

        String resp = HttpUtil.postForm(
                HOST_API + "/x/dynamic/feed/create/dyn",
                signed, buildCommonHeaders());
        return parse(resp);
    }

    /**
     * 在视频下发表评论。
     *
     * @param oid  视频 AV 号对应的 oid（参考版本直接用传入数字）
     * @param msg  评论内容
     */
    public JSONObject addReply(String oid, String msg) {
        if (!account.isLoggedIn()) {
            return null;
        }
        TreeMap params = new TreeMap();
        params.put("oid", oid == null ? "0" : oid);
        params.put("type", "1");
        params.put("message", msg == null ? "" : msg);
        params.put("access_key", account.getAccessToken());
        TreeMap signed = SignUtil.signWithAppkey(params);

        String resp = HttpUtil.postForm(
                HOST_API + "/x/v2/reply/add",
                signed, buildCommonHeaders());
        return parse(resp);
    }

    /** 登出。返回是否成功。 */
    public boolean logout() {
        if (!account.isLoggedIn()) {
            return true;
        }
        TreeMap params = new TreeMap();
        params.put("access_key", account.getAccessToken());
        TreeMap signed = SignUtil.signWithAppkey(params);
        String resp = HttpUtil.postForm(
                HOST_PASSPORT + "/login/exit/v2",
                signed, buildCommonHeaders());
        JSONObject json = parse(resp);
        return json != null && json.optInt("code", -1) == 0;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private HashMap buildCommonHeaders() {
        HashMap headers = new HashMap();
        headers.put("Referer", "https://www.bilibili.com/");
        return headers;
    }

    private JSONObject parse(String body) {
        if (body == null || body.length() == 0) {
            return null;
        }
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            return null;
        }
    }
}
