/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.api;

import android.util.Log;

import com.hibili.app.util.Md5Util;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * WBI sign — used by web endpoints.
 *
 * 1) fetch /x/web-interface/nav once per day to obtain wbi_img.img_url + sub_url
 * 2) take filenames (no extension) and concatenate → raw_mixin_key
 * 3) reorder through the fixed MIXIN_KEY_ENC_TAB → mixin_key (32 chars)
 * 4) sort params, URL-encode, append wts → md5(query + mixin_key) = w_rid
 */
public final class WbiSign {

    private static final String TAG = "WbiSign";

    private static final int[] MIXIN_KEY_ENC_TAB = new int[]{
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    private static String cachedMixinKey;
    private static long cachedDay;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private WbiSign() {}

    public static synchronized void ensureMixinKey() {
        long today = System.currentTimeMillis() / 86400000L;
        if (cachedMixinKey != null && today == cachedDay) return;
        try {
            Request req = new Request.Builder()
                    .url("https://api.bilibili.com/x/web-interface/nav")
                    .header("User-Agent", "Mozilla/5.0 (HiBili)")
                    .build();
            try (Response resp = HTTP.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "{}";
                String img = extract(body, "\"img_url\":\"", "\"");
                String sub = extract(body, "\"sub_url\":\"", "\"");
                String raw = filename(img) + filename(sub);
                cachedMixinKey = buildMixinKey(raw);
                cachedDay = today;
                Log.i(TAG, "mixin key refreshed");
            }
        } catch (IOException e) {
            Log.w(TAG, "nav fetch failed, keeping previous key", e);
        }
    }

    /**
     * Injects wts and w_rid into {@code params}.
     */
    public static void sign(TreeMap<String, String> params) {
        ensureMixinKey();
        params.put("wts", String.valueOf(System.currentTimeMillis() / 1000L));

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(e.getKey()).append('=').append(urlEncode(e.getValue()));
        }
        String wRid = Md5Util.md5(sb.toString() + (cachedMixinKey == null ? "" : cachedMixinKey));
        params.put("w_rid", wRid);
    }

    private static String buildMixinKey(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int idx : MIXIN_KEY_ENC_TAB) {
            if (idx < raw.length()) sb.append(raw.charAt(idx));
            if (sb.length() == 32) break;
        }
        return sb.toString();
    }

    private static String filename(String url) {
        if (url == null) return "";
        int slash = url.lastIndexOf('/');
        String tail = slash >= 0 ? url.substring(slash + 1) : url;
        int dot = tail.indexOf('.');
        return dot >= 0 ? tail.substring(0, dot) : tail;
    }

    private static String extract(String body, String start, String end) {
        int i = body.indexOf(start);
        if (i < 0) return "";
        int j = body.indexOf(end, i + start.length());
        if (j < 0) return "";
        return body.substring(i + start.length(), j).replace("\\/", "/");
    }

    /** Minimal URL encoder that matches Bilibili's expectation. */
    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8")
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return s;
        }
    }
}
