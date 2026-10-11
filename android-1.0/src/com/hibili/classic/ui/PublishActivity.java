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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hibili.classic.HiBiliApp;
import com.hibili.classic.R;
import com.hibili.classic.account.AccountManager;
import com.hibili.classic.api.BiliApi;

import org.json.JSONObject;

/**
 * 发布文字动态。
 */
public class PublishActivity extends Activity {

    private static final int MSG_DONE = 1;

    private EditText etText;
    private BiliApi api;
    private AccountManager account;

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            boolean ok = msg.arg1 == 1;
            Toast.makeText(PublishActivity.this,
                    ok ? R.string.toast_publish_ok : R.string.toast_publish_fail,
                    Toast.LENGTH_SHORT).show();
            if (ok) {
                finish();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.publish);

        api = HiBiliApp.getApi();
        account = HiBiliApp.getAccount();

        etText = (EditText) findViewById(R.id.et_dynamic_text);
        Button btnPublish = (Button) findViewById(R.id.btn_publish);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                publish();
            }
        });
    }

    private void publish() {
        if (!account.isLoggedIn()) {
            Toast.makeText(this, R.string.toast_not_login, Toast.LENGTH_SHORT).show();
            return;
        }
        final String text = etText.getText().toString().trim();
        if (text.length() == 0) {
            Toast.makeText(this, R.string.toast_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                JSONObject resp = api.createTextDynamic(text);
                boolean ok = resp != null && resp.optInt("code", -1) == 0;
                handler.sendMessage(handler.obtainMessage(MSG_DONE, ok ? 1 : 0, 0));
            }
        }).start();
    }
}
