/*
 * This file is part of Hi！Bili.
 *
 * Hi！Bili is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hi！Bili is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hi！Bili.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.hibili.classic.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.hibili.classic.HiBiliApp;
import com.hibili.classic.R;
import com.hibili.classic.account.AccountManager;

/**
 * 主页：显示登录状态与功能入口。
 */
public class MainActivity extends Activity {

    private TextView tvStatus;
    private AccountManager account;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        account = HiBiliApp.getAccount();
        tvStatus = (TextView) findViewById(R.id.tv_login_status);

        Button btnPublish = (Button) findViewById(R.id.btn_publish_dynamic);
        Button btnReply = (Button) findViewById(R.id.btn_reply);
        Button btnAbout = (Button) findViewById(R.id.btn_about);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!ensureLogin()) {
                    return;
                }
                startActivity(new Intent(MainActivity.this, PublishActivity.class));
            }
        });
        btnReply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!ensureLogin()) {
                    return;
                }
                startActivity(new Intent(MainActivity.this, ReplyActivity.class));
            }
        });
        btnAbout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
    }

    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        if (account.isLoggedIn()) {
            String uname = account.getUname();
            if (uname == null || uname.length() == 0) {
                uname = account.getMid();
            }
            tvStatus.setText(String.format(
                    getString(R.string.main_status_login_fmt), uname));
        } else {
            tvStatus.setText(R.string.main_status_not_login);
        }
    }

    private boolean ensureLogin() {
        if (account.isLoggedIn()) {
            return true;
        }
        startActivity(new Intent(this, LoginActivity.class));
        return false;
    }
}
