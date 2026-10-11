/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.account;

import android.content.Context;
import android.content.SharedPreferences;

import com.hibili.app.model.UserInfo;

/**
 * Process-wide singleton that holds the logged-in user and persists the
 * credential set into SharedPreferences. Every outgoing request reads the
 * current cookie / access_key from here.
 */
public final class AccountManager {

    private static final String PREF = "hibili_account";
    private static final String K_AK = "access_token";
    private static final String K_RK = "refresh_token";
    private static final String K_CSRF = "csrf";
    private static final String K_MID = "mid";
    private static final String K_NAME = "name";
    private static final String K_FACE = "face";
    private static final String K_COOKIES = "cookies";

    private static AccountManager instance;

    private final SharedPreferences sp;
    private UserInfo current;

    private AccountManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        current = new UserInfo();
        current.accessKey = sp.getString(K_AK, null);
        current.refreshKey = sp.getString(K_RK, null);
        current.csrf = sp.getString(K_CSRF, null);
        current.mid = sp.getLong(K_MID, 0L);
        current.name = sp.getString(K_NAME, null);
        current.face = sp.getString(K_FACE, null);
        current.cookies = sp.getString(K_COOKIES, null);
    }

    public static synchronized AccountManager get(Context ctx) {
        if (instance == null) instance = new AccountManager(ctx);
        return instance;
    }

    public synchronized UserInfo getCurrent() {
        return current;
    }

    public synchronized boolean isLoggedIn() {
        return current.accessKey != null && current.csrf != null;
    }

    public synchronized void save(UserInfo info) {
        this.current = info;
        SharedPreferences.Editor e = sp.edit();
        if (info.accessKey != null) e.putString(K_AK, info.accessKey);
        if (info.refreshKey != null) e.putString(K_RK, info.refreshKey);
        if (info.csrf != null) e.putString(K_CSRF, info.csrf);
        e.putLong(K_MID, info.mid);
        if (info.name != null) e.putString(K_NAME, info.name);
        if (info.face != null) e.putString(K_FACE, info.face);
        if (info.cookies != null) e.putString(K_COOKIES, info.cookies);
        e.apply();
    }

    public synchronized void logout() {
        current = new UserInfo();
        sp.edit().clear().apply();
    }

    /** Builds the Cookie request header from the persisted cookie string. */
    public synchronized String cookieHeader() {
        return current.cookies == null ? "" : current.cookies;
    }
}
