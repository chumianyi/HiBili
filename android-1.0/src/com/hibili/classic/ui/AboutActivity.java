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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hibili.classic.HiBiliApp;
import com.hibili.classic.R;
import com.hibili.classic.account.AccountManager;
import com.hibili.classic.api.BiliApi;

/**
 * 关于：应用信息 / GPL 声明 / 致谢 / 退出登录。
 */
public class AboutActivity extends Activity {

    private static final int MSG_LOGOUT_DONE = 1;

    private BiliApi api;
    private AccountManager account;

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(AboutActivity.this,
                    R.string.toast_logout_ok, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AboutActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        api = HiBiliApp.getApi();
        account = HiBiliApp.getAccount();

        Button btnLogout = (Button) findViewById(R.id.btn_logout);
        Button btnBack = (Button) findViewById(R.id.btn_back);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                confirmLogout();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_exit_title)
                .setMessage(R.string.dialog_exit_msg)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        doLogout();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void doLogout() {
        new Thread(new Runnable() {
            public void run() {
                api.logout();
                account.clear();
                handler.sendEmptyMessage(MSG_LOGOUT_DONE);
            }
        }).start();
    }
}
