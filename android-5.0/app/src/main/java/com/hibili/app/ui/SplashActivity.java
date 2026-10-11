/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.hibili.app.R;
import com.hibili.app.account.AccountManager;

/** Splash: blue gradient + logo, 2s, then route to Main or Login. */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                Class<?> target = AccountManager.get(SplashActivity.this).isLoggedIn()
                        ? MainActivity.class : LoginActivity.class;
                startActivity(new Intent(SplashActivity.this, target));
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, 2000L);
    }
}
