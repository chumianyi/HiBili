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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hibili.app.R;
import com.hibili.app.account.AccountManager;
import com.hibili.app.model.UserInfo;

/**
 * Main shell: user card + feature entries + bottom nav.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private ImageView ivAvatar;
    private TextView tvNick, tvMid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivAvatar = findViewById(R.id.iv_avatar);
        tvNick = findViewById(R.id.tv_nickname);
        tvMid = findViewById(R.id.tv_mid);

        findViewById(R.id.btn_publish).setOnClickListener(this);
        findViewById(R.id.btn_reply).setOnClickListener(this);
        findViewById(R.id.btn_about).setOnClickListener(this);

        findViewById(R.id.nav_home).setOnClickListener(this);
        findViewById(R.id.nav_dynamic).setOnClickListener(this);
        findViewById(R.id.nav_me).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        UserInfo u = AccountManager.get(this).getCurrent();
        if (u != null && u.accessKey != null) {
            tvNick.setText(u.name != null && !u.name.isEmpty() ? u.name : getString(R.string.welcome_back));
            tvMid.setText(u.mid > 0 ? "mid: " + u.mid : "");
        } else {
            tvNick.setText(R.string.not_logged_in);
            tvMid.setText("");
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_publish) open(PublishActivity.class);
        else if (id == R.id.btn_reply) open(ReplyActivity.class);
        else if (id == R.id.btn_about) open(AboutActivity.class);
        else if (id == R.id.nav_dynamic) open(PublishActivity.class);
        else if (id == R.id.nav_me) open(AboutActivity.class);
        // nav_home: stay
    }

    private void open(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
