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
 * 发表评论。
 */
public class ReplyActivity extends Activity {

    private static final int MSG_DONE = 1;

    private EditText etVideoId;
    private EditText etContent;
    private BiliApi api;
    private AccountManager account;

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            boolean ok = msg.arg1 == 1;
            Toast.makeText(ReplyActivity.this,
                    ok ? R.string.toast_publish_ok : R.string.toast_publish_fail,
                    Toast.LENGTH_SHORT).show();
            if (ok) {
                finish();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reply);

        api = HiBiliApp.getApi();
        account = HiBiliApp.getAccount();

        etVideoId = (EditText) findViewById(R.id.et_video_id);
        etContent = (EditText) findViewById(R.id.et_reply_content);
        Button btnSend = (Button) findViewById(R.id.btn_send_reply);

        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendReply();
            }
        });
    }

    private void sendReply() {
        if (!account.isLoggedIn()) {
            Toast.makeText(this, R.string.toast_not_login, Toast.LENGTH_SHORT).show();
            return;
        }
        final String oid = etVideoId.getText().toString().trim();
        final String content = etContent.getText().toString().trim();
        if (oid.length() == 0 || content.length() == 0) {
            Toast.makeText(this, R.string.toast_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                JSONObject resp = api.addReply(oid, content);
                boolean ok = resp != null && resp.optInt("code", -1) == 0;
                handler.sendMessage(handler.obtainMessage(MSG_DONE, ok ? 1 : 0, 0));
            }
        }).start();
    }
}
