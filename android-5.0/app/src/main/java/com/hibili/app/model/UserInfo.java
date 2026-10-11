/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.model;

/** User profile summary. Mirrors fields returned by /x/passport-tv-login/poll and /x/web-interface/nav. */
public class UserInfo {
    public long mid;
    public String name;
    public String face;
    public String accessKey;
    public String refreshKey;
    public String cookies; // raw "k=v; k2=v2"
    public String csrf;    // = bili_jct
}
