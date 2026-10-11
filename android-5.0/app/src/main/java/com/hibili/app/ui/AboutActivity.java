/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hibili.app.R;
import com.hibili.app.account.AccountManager;
import com.hibili.app.api.ApiCallback;
import com.hibili.app.api.BiliApi;

/**
 * About page: icon, version, repo, license, thanks list, logout button.
 */
public class AboutActivity extends Activity {

    // Acknowledgements — factual references to open-source projects this app
    // builds on. Names are kept verbatim per GPL attribution requirements.
    private static final String[][] THANKS = new String[][]{
            {"PiliPlus", "https://github.com/bggRGjQaUbCoE/PiliPlus"},
            {"OkHttp",   "https://github.com/square/okhttp"},
            {"Gson",     "https://github.com/google/gson"},
            {"zxing",    "https://github.com/zxing/zxing"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        LinearLayout container = findViewById(R.id.thanks_container);
        for (int i = 0; i < THANKS.length; i++) {
            TextView name = new TextView(this);
            name.setText(THANKS[i][0]);
            name.setTextSize(16);
            name.setTextColor(getResources().getColor(R.color.text_primary));
            name.setPadding(0, i == 0 ? 0 : 16, 0, 0);

            TextView url = new TextView(this);
            url.setText(THANKS[i][1]);
            url.setTextSize(13);
            url.setTextColor(getResources().getColor(R.color.miuix_primary));

            container.addView(name);
            container.addView(url);
        }

        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                BiliApi.with(AboutActivity.this).logout(new ApiCallback<Boolean>() {
                    @Override public void onSuccess(Boolean data) {
                        Toast.makeText(AboutActivity.this, R.string.toast_logout_ok, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AboutActivity.this, LoginActivity.class));
                        finish();
                    }
                    @Override public void onError(int code, String message) {
                        AccountManager.get(AboutActivity.this).logout();
                        Toast.makeText(AboutActivity.this, R.string.toast_logout_ok, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AboutActivity.this, LoginActivity.class));
                        finish();
                    }
                });
            }
        });
    }
}
