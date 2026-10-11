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
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.hibili.app.R;
import com.hibili.app.account.AccountManager;
import com.hibili.app.api.ApiCallback;
import com.hibili.app.api.BiliApi;

/**
 * Publish a text dynamic. Image attach is exposed as a button (intent picker
 * stub) but the upload endpoint is wired in BiliApi; the text-only scene=1
 * path is the one exercised by the publish button.
 */
public class PublishActivity extends Activity {

    private EditText etContent;
    private Button btnPublish;
    private TextView btnAddImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        etContent = findViewById(R.id.et_content);
        btnPublish = findViewById(R.id.btn_publish);
        btnAddImage = findViewById(R.id.btn_add_image);

        if (!AccountManager.get(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_not_login, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(i, getString(R.string.publish_add_image)), 100);
                } catch (Exception ignored) {}
            }
        });

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String text = etContent.getText().toString().trim();
                if (TextUtils.isEmpty(text)) {
                    Toast.makeText(PublishActivity.this, R.string.publish_hint, Toast.LENGTH_SHORT).show();
                    return;
                }
                btnPublish.setEnabled(false);
                BiliApi.with(PublishActivity.this).publishDynamic(text, new ApiCallback<JsonObject>() {
                    @Override public void onSuccess(JsonObject data) {
                        btnPublish.setEnabled(true);
                        Toast.makeText(PublishActivity.this, R.string.toast_publish_ok, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onError(int code, String message) {
                        btnPublish.setEnabled(true);
                        Toast.makeText(PublishActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
