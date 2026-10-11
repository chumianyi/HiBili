/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.hibili.app.R;
import com.hibili.app.account.AccountManager;
import com.hibili.app.api.ApiCallback;
import com.hibili.app.api.BiliApi;

/**
 * Post a comment on a video (type=1). Accepts BV or AV number as the oid.
 */
public class ReplyActivity extends Activity {

    private EditText etBv, etMessage;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);

        etBv = findViewById(R.id.et_bvid);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        if (!AccountManager.get(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_not_login, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String oid = etBv.getText().toString().trim();
                String msg = etMessage.getText().toString().trim();
                if (TextUtils.isEmpty(oid) || TextUtils.isEmpty(msg)) {
                    Toast.makeText(ReplyActivity.this, R.string.reply_hint_content, Toast.LENGTH_SHORT).show();
                    return;
                }
                btnSend.setEnabled(false);
                BiliApi.with(ReplyActivity.this).postReply(1, oid, msg, new ApiCallback<JsonObject>() {
                    @Override public void onSuccess(JsonObject data) {
                        btnSend.setEnabled(true);
                        Toast.makeText(ReplyActivity.this, R.string.toast_publish_ok, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onError(int code, String message) {
                        btnSend.setEnabled(true);
                        Toast.makeText(ReplyActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
