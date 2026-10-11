/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.model;

/** Raw payload of a successful TV qrcode poll. */
public class LoginResult {
    public long mid;
    public String access_token;
    public String refresh_token;
    public String cookie_info; // raw cookie string
    public String uname;
    public String face;
}
