/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app;

import android.app.Application;

import com.hibili.app.account.AccountManager;
import com.hibili.app.api.BiliApi;

/**
 * Application entry point. Warms up the account singleton and the HTTP stack
 * so every Activity can rely on them being initialized.
 */
public class HiBiliApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AccountManager.get(this);
        BiliApi.with(this);
    }
}
