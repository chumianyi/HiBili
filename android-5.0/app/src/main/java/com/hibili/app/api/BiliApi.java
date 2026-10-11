/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hibili.app.account.AccountManager;
import com.hibili.app.model.LoginResult;
import com.hibili.app.model.UserInfo;
import com.hibili.app.util.RsaUtil;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Bilibili API facade. Every method spawns a background thread; the caller's
 * {@link ApiCallback} is always invoked on the main thread.
 */
public final class BiliApi {

    private static final String TAG = "BiliApi";

    public static final String BASE_API = "https://api.bilibili.com";
    public static final String BASE_PASSPORT = "https://passport.bilibili.com";
    public static final String BASE_APP = "https://app.bilibili.com";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    private static volatile BiliApi instance;
    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context ctx;
    private final AccountManager account;

    private BiliApi(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.account = AccountManager.get(this.ctx);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public static BiliApi with(Context ctx) {
        if (instance == null) {
            synchronized (BiliApi.class) {
                if (instance == null) instance = new BiliApi(ctx);
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // TV qrcode login
    // ------------------------------------------------------------------

    /** Requests an auth_code + QR url. On success returns the data JsonObject. */
    public void tvQrcodeAuthCode(final ApiCallback<JsonObject> cb) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("local_id", "0");
        params.put("platform", "android");
        params.put("mobi_app", "android_hd");
        AppSign.sign(params);
        postForm(BASE_PASSPORT + "/x/passport-tv-login/qrcode/auth_code", params, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) { fail(cb, code, root); return; }
                succeed(cb, root.getAsJsonObject("data"));
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    /** Polls the TV qrcode state. code == 0 → logged in. */
    public void tvQrcodePoll(final String authCode, final ApiCallback<LoginResult> cb) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("auth_code", authCode);
        params.put("local_id", "0");
        AppSign.sign(params);
        postForm(BASE_PASSPORT + "/x/passport-tv-login/qrcode/poll", params, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) {
                    // -86 二维码未失效 / -86 etc; propagate non-zero codes so UI can react
                    fail(cb, code, root);
                    return;
                }
                JsonObject data = root.getAsJsonObject("data");
                LoginResult r = gson.fromJson(data, LoginResult.class);
                String cookieStr = data.has("cookie_info") && data.getAsJsonObject("cookie_info").has("cookies")
                        ? flattenCookies(data.getAsJsonObject("cookie_info").getAsJsonObject("cookies").getAsJsonArray().toString())
                        : "";
                UserInfo u = new UserInfo();
                u.mid = r.mid;
                u.accessKey = r.access_token;
                u.refreshKey = r.refresh_token;
                u.cookies = cookieStr;
                u.csrf = extractCookie(cookieStr, "bili_jct");
                u.name = r.uname;
                u.face = r.face;
                account.save(u);
                succeed(cb, r);
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    // ------------------------------------------------------------------
    // Password login
    // ------------------------------------------------------------------

    public void passwordLogin(final String username, final String password, final ApiCallback<UserInfo> cb) {
        // 1) fetch RSA key
        getJson(BASE_API + "/x/passport-login/web/key", null, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) { fail(cb, code, root); return; }
                JsonObject data = root.getAsJsonObject("data");
                String hash = data.get("hash").getAsString();
                String key = data.get("key").getAsString();
                try {
                    String enc = RsaUtil.encryptBase64(key, hash + password);
                    doOauthLogin(username, enc, cb);
                } catch (Exception e) {
                    error(cb, e);
                }
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    private void doOauthLogin(String username, String encPassword, final ApiCallback<UserInfo> cb) {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("username", username);
        params.put("password", encPassword);
        params.put("buvid", "");
        params.put("device_id", "");
        params.put("mobi_app", "android_hd");
        params.put("platform", "android");
        AppSign.sign(params);
        postForm(BASE_PASSPORT + "/x/passport-login/oauth2/login", params, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) { fail(cb, code, root); return; }
                JsonObject data = root.getAsJsonObject("data");
                UserInfo u = new UserInfo();
                if (data.has("access_token")) u.accessKey = data.get("access_token").getAsString();
                if (data.has("refresh_token")) u.refreshKey = data.get("refresh_token").getAsString();
                if (data.has("cookie_info")) {
                    String cookies = flattenCookies(data.getAsJsonObject("cookie_info").getAsJsonObject("cookies").getAsJsonArray().toString());
                    u.cookies = cookies;
                    u.csrf = extractCookie(cookies, "bili_jct");
                    u.mid = safeLong(extractCookie(cookies, "DedeUserID"));
                }
                account.save(u);
                succeed(cb, u);
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    // ------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------

    public void logout(final ApiCallback<Boolean> cb) {
        if (!account.isLoggedIn()) { succeed(cb, Boolean.TRUE); return; }
        TreeMap<String, String> form = new TreeMap<>();
        form.put("biliCSRF", account.getCurrent().csrf == null ? "" : account.getCurrent().csrf);
        postJson(BASE_PASSPORT + "/login/exit/v2", gson.toJson(form), new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                account.logout();
                succeed(cb, Boolean.TRUE);
            }
            @Override public void onFail(IOException e) {
                account.logout();
                succeed(cb, Boolean.TRUE);
            }
        });
    }

    // ------------------------------------------------------------------
    // Publish dynamic
    // ------------------------------------------------------------------

    public void publishDynamic(final String text, final ApiCallback<JsonObject> cb) {
        if (!account.isLoggedIn()) { fail(cb, -1, "not logged in"); return; }
        long mid = account.getCurrent().mid;
        String uploadId = mid + "_" + (System.currentTimeMillis() / 1000L) + "_" + (int)(Math.random() * 10000);

        String body = "{"
                + "\"dyn_req\":{"
                + "\"content\":{\"contents\":[{\"raw_text\":\"" + escapeJson(text) + "\",\"type\":1}]},"
                + "\"scene\":1,"
                + "\"upload_id\":\"" + uploadId + "\","
                + "\"meta\":{\"app_meta\":{\"from\":\"create.dynamic.web\",\"mobi_app\":\"web\"}},"
                + "\"option\":{\"private_pub\":0,\"timer_pub_time\":0,\"close_comment\":0}"
                + "}}";

        String url = BASE_API + "/x/dynamic/feed/create/dyn?platform=web&csrf="
                + account.getCurrent().csrf + "&wts=" + (System.currentTimeMillis()/1000L);
        postJson(url, body, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) { fail(cb, code, root); return; }
                succeed(cb, root);
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    // ------------------------------------------------------------------
    // Post comment
    // ------------------------------------------------------------------

    /**
     * type=1 (video) / oid = video av/aid. BV number is accepted by the web
     * endpoint; we pass it through as-is.
     */
    public void postReply(final int type, final String oid, final String message, final ApiCallback<JsonObject> cb) {
        if (!account.isLoggedIn()) { fail(cb, -1, "not logged in"); return; }
        TreeMap<String, String> form = new TreeMap<>();
        form.put("type", String.valueOf(type));
        form.put("oid", oid);
        form.put("message", message);
        form.put("csrf", account.getCurrent().csrf);
        form.put("sync_to_dynamic", "1");
        postForm(BASE_API + "/x/v2/reply/add", form, new RawCallback() {
            @Override public void onRaw(JsonObject root) {
                int code = root.get("code").getAsInt();
                if (code != 0) { fail(cb, code, root); return; }
                succeed(cb, root);
            }
            @Override public void onFail(IOException e) { error(cb, e); }
        });
    }

    // ------------------------------------------------------------------
    // Low-level HTTP plumbing
    // ------------------------------------------------------------------

    private interface RawCallback {
        void onRaw(JsonObject root);
        void onFail(IOException e);
    }

    private void getJson(String url, TreeMap<String, String> query, final RawCallback cb) {
        final String full = buildUrl(url, query);
        Runnable r = new Runnable() {
            @Override public void run() {
                Request req = new Request.Builder()
                        .url(full)
                        .header("User-Agent", "Mozilla/5.0 (HiBili)")
                        .header("Cookie", account.cookieHeader())
                        .get()
                        .build();
        call(req, cb);
            }
        };
        new Thread(r).start();
    }

    private void postForm(String url, TreeMap<String, String> form, final RawCallback cb) {
        final String full = url;
        FormBody.Builder fb = new FormBody.Builder();
        for (Map.Entry<String, String> e : form.entrySet()) fb.add(e.getKey(), e.getValue());
        final RequestBody body = fb.build();
        Runnable r = new Runnable() {
            @Override public void run() {
                Request req = new Request.Builder()
                        .url(full)
                        .header("User-Agent", "Mozilla/5.0 (HiBili)")
                        .header("Cookie", account.cookieHeader())
                        .post(body)
                        .build();
                call(req, cb);
            }
        };
        new Thread(r).start();
    }

    private void postJson(String url, String json, final RawCallback cb) {
        final RequestBody body = RequestBody.create(JSON, json);
        Runnable r = new Runnable() {
            @Override public void run() {
                Request req = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (HiBili)")
                        .header("Cookie", account.cookieHeader())
                        .header("Content-Type", "application/json; charset=utf-8")
                        .post(body)
                        .build();
                call(req, cb);
            }
        };
        new Thread(r).start();
    }

    private void call(Request req, final RawCallback cb) {
        try (Response resp = http.newCall(req).execute()) {
            String text = resp.body() != null ? resp.body().string() : "{}";
            Log.d(TAG, req.url() + " -> " + resp.code() + " " + text);
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            cb.onRaw(root);
        } catch (IOException e) {
            cb.onFail(e);
        } catch (Exception e) {
            cb.onFail(new IOException(e));
        }
    }

    private String buildUrl(String base, TreeMap<String, String> query) {
        if (query == null || query.isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String flattenCookies(String cookiesJson) {
        // cookiesJson is [{"name":"bili_jct","value":"...",...}, ...]
        // We re-parse via Gson to keep things simple.
        try {
            com.google.gson.JsonArray arr = JsonParser.parseString(cookiesJson).getAsJsonArray();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                if (i > 0) sb.append("; ");
                sb.append(o.get("name").getAsString()).append('=').append(o.get("value").getAsString());
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String extractCookie(String cookieStr, String name) {
        if (TextUtils.isEmpty(cookieStr)) return "";
        for (String piece : cookieStr.split(";")) {
            int eq = piece.indexOf('=');
            if (eq < 0) continue;
            if (piece.substring(0, eq).trim().equals(name)) {
                return piece.substring(eq + 1).trim();
            }
        }
        return "";
    }

    private static long safeLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    private <T> void succeed(final ApiCallback<T> cb, final T data) {
        main.post(new Runnable() {
            @Override public void run() { cb.onSuccess(data); }
        });
    }

    private void fail(final ApiCallback<?> cb, int code, JsonObject root) {
        String msg = root.has("message") ? root.get("message").getAsString()
                : (root.has("data") && root.getAsJsonObject("data").has("message")
                    ? root.getAsJsonObject("data").get("message").getAsString()
                    : "error");
        fail(cb, code, msg);
    }

    private void fail(final ApiCallback<?> cb, final int code, final String msg) {
        main.post(new Runnable() {
            @Override public void run() { cb.onError(code, msg); }
        });
    }

    private void error(final ApiCallback<?> cb, final Throwable e) {
        main.post(new Runnable() {
            @Override public void run() { cb.onError(-1, e.getMessage() == null ? "network error" : e.getMessage()); }
        });
    }
}
