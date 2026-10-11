/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.api;

import com.hibili.app.util.Md5Util;

import java.util.Map;
import java.util.TreeMap;

/**
 * AppKey sign — used by passport / app endpoints.
 *
 *   appkey = dfca71928277209b
 *   appsec = b5475a8825547a4fc26c7d518eaaa02e
 *
 * sign = md5( sortedQueryWithAppkeyAndTs + appsec )
 */
public final class AppSign {

    public static final String APPKEY = "dfca71928277209b";
    public static final String APPSEC = "b5475a8825547a4fc26c7d518eaaa02e";

    private AppSign() {}

    /**
     * Mutates {@code params} by inserting {@code appkey}, {@code ts} and {@code sign}.
     */
    public static void sign(TreeMap<String, String> params) {
        params.put("appkey", APPKEY);
        long ts = System.currentTimeMillis() / 1000L;
        params.put("ts", String.valueOf(ts));

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append('&');
            first = false;
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        String sign = Md5Util.md5(sb.toString() + APPSEC);
        params.put("sign", sign);
    }
}
