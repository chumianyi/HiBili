/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.hibili.app.R;
import com.hibili.app.api.ApiCallback;
import com.hibili.app.api.BiliApi;
import com.hibili.app.model.LoginResult;
import com.hibili.app.model.UserInfo;
import com.hibili.app.util.QrCodeUtil;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Two-tab login: TV qrcode polling, or username + password.
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    private TextView tabQrcode, tabPassword;
    private LinearLayout paneQrcode, panePassword;

    private ImageView qrImage;
    private TextView qrStatus;

    private EditText etUser, etPass;
    private Button btnLogin;

    private String authCode;
    private Timer pollTimer;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tabQrcode = findViewById(R.id.tab_qrcode);
        tabPassword = findViewById(R.id.tab_password);
        paneQrcode = findViewById(R.id.pane_qrcode);
        panePassword = findViewById(R.id.pane_password);
        qrImage = findViewById(R.id.qrcode_image);
        qrStatus = findViewById(R.id.qrcode_status);
        etUser = findViewById(R.id.et_username);
        etPass = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        tabQrcode.setOnClickListener(this);
        tabPassword.setOnClickListener(this);
        btnLogin.setOnClickListener(this);

        switchTab(true);
        startQrFlow();
    }

    @Override
    protected void onDestroy() {
        stopPolling();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tab_qrcode) switchTab(true);
        else if (id == R.id.tab_password) switchTab(false);
        else if (id == R.id.btn_login) doPasswordLogin();
    }

    private void switchTab(boolean qr) {
        paneQrcode.setVisibility(qr ? View.VISIBLE : View.GONE);
        panePassword.setVisibility(qr ? View.GONE : View.VISIBLE);
        tabQrcode.setTextColor(getResources().getColor(qr ? R.color.miuix_primary : R.color.text_secondary));
        tabPassword.setTextColor(getResources().getColor(qr ? R.color.text_secondary : R.color.miuix_primary));
        tabQrcode.setTypeface(tabQrcode.getTypeface(), qr ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabPassword.setTypeface(tabPassword.getTypeface(), qr ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
    }

    // ------ QR flow ---------------------------------------------------

    private void startQrFlow() {
        qrStatus.setText(R.string.qrcode_wait_scan);
        BiliApi.with(this).tvQrcodeAuthCode(new ApiCallback<JsonObject>() {
            @Override public void onSuccess(JsonObject data) {
                authCode = data.has("auth_code") ? data.get("auth_code").getAsString() : null;
                String url = data.has("url") ? data.get("url").getAsString() : "";
                final Bitmap bmp = QrCodeUtil.encode(url, 600);
                ui.post(new Runnable() {
                    @Override public void run() { qrImage.setImageBitmap(bmp); }
                });
                startPolling();
            }
            @Override public void onError(int code, String message) {
                qrStatus.setText(message);
            }
        });
    }

    private void startPolling() {
        stopPolling();
        pollTimer = new Timer("qr-poll");
        pollTimer.schedule(new TimerTask() {
            @Override public void run() {
                if (TextUtils.isEmpty(authCode)) return;
                BiliApi.with(LoginActivity.this).tvQrcodePoll(authCode, new ApiCallback<LoginResult>() {
                    @Override public void onSuccess(LoginResult data) {
                        ui.post(new Runnable() {
                            @Override public void run() {
                                Toast.makeText(LoginActivity.this, R.string.toast_login_ok, Toast.LENGTH_SHORT).show();
                                gotoMain();
                            }
                        });
                    }
                    @Override public void onError(int code, String message) {
                        // -85 二维码过期 / -50 未扫码超时等
                        if (code == -85 || code == -50) {
                            ui.post(new Runnable() {
                                @Override public void run() {
                                    qrStatus.setText(R.string.qrcode_expired);
                                }
                            });
                        }
                    }
                });
            }
        }, 0, 3000L);
    }

    private void stopPolling() {
        if (pollTimer != null) { pollTimer.cancel(); pollTimer = null; }
    }

    // ------ Password flow ---------------------------------------------

    private void doPasswordLogin() {
        String u = etUser.getText().toString().trim();
        String p = etPass.getText().toString();
        if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
            Toast.makeText(this, R.string.toast_login_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        btnLogin.setEnabled(false);
        BiliApi.with(this).passwordLogin(u, p, new ApiCallback<UserInfo>() {
            @Override public void onSuccess(UserInfo data) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, R.string.toast_login_ok, Toast.LENGTH_SHORT).show();
                gotoMain();
            }
            @Override public void onError(int code, String message) {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void gotoMain() {
        stopPolling();
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
