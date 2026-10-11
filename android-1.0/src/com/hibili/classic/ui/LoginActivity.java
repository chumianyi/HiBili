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
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hibili.classic.HiBiliApp;
import com.hibili.classic.R;
import com.hibili.classic.account.AccountManager;
import com.hibili.classic.api.BiliApi;

import org.json.JSONObject;

/**
 * 登录页：TV 扫码 + 密码登录。
 * 网络请求在子线程执行，通过 Handler 回主线程更新 UI。
 */
public class LoginActivity extends Activity {

    private static final int MSG_QR_READY = 1;
    private static final int MSG_QR_RESULT = 2;
    private static final int MSG_LOGIN_RESULT = 3;

    private WebView qrWeb;
    private TextView qrHint;
    private Button btnRefreshQr;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    private BiliApi api;
    private AccountManager account;

    private String currentAuthCode;
    private boolean polling;
    private boolean stopping;

    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_QR_READY: {
                    String imgUrl = (String) msg.obj;
                    if (imgUrl != null && imgUrl.length() > 0) {
                        // WebView 直接加载二维码图片 URL
                        qrWeb.loadUrl(imgUrl);
                        qrHint.setText(R.string.qrcode_wait);
                    } else {
                        qrHint.setText(R.string.toast_network_err);
                    }
                    break;
                }
                case MSG_QR_RESULT: {
                    int state = msg.arg1;
                    if (state == 0) {
                        onLoginSuccess((JSONObject) msg.obj);
                    } else if (state == 86038) {
                        qrHint.setText(R.string.qrcode_wait);
                    } else if (state == 86039) {
                        qrHint.setText(R.string.qrcode_scanned);
                    } else {
                        // 二维码失效或失败，停止轮询
                        polling = false;
                        qrHint.setText(R.string.toast_login_fail);
                    }
                    break;
                }
                case MSG_LOGIN_RESULT: {
                    boolean ok = msg.arg1 == 1;
                    if (ok) {
                        onLoginSuccess((JSONObject) msg.obj);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                R.string.toast_login_fail, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        api = HiBiliApp.getApi();
        account = HiBiliApp.getAccount();

        qrWeb = (WebView) findViewById(R.id.qrcode_web);
        qrHint = (TextView) findViewById(R.id.qrcode_hint);
        btnRefreshQr = (Button) findViewById(R.id.btn_refresh_qrcode);
        etUsername = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLogin = (Button) findViewById(R.id.btn_login);

        btnRefreshQr.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startQrLogin();
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doPasswordLogin();
            }
        });

        startQrLogin();
    }

    // ------------------------------------------------------------------
    // 扫码登录
    // ------------------------------------------------------------------

    private void startQrLogin() {
        qrHint.setText(R.string.toast_network_err);
        new Thread(new Runnable() {
            public void run() {
                JSONObject data = api.requestTvAuthCode();
                if (data == null) {
                    handler.sendMessage(handler.obtainMessage(MSG_QR_READY, ""));
                    return;
                }
                currentAuthCode = data.optString("auth_code", "");
                String imgUrl = data.optString("qrcode_img_url", "");
                handler.sendMessage(handler.obtainMessage(MSG_QR_READY, imgUrl));

                // 启动轮询
                polling = true;
                while (polling && !stopping) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                    JSONObject resp = api.pollTvAuthCode(currentAuthCode);
                    if (resp == null) {
                        continue;
                    }
                    int code = resp.optInt("code", -1);
                    Message m = handler.obtainMessage(MSG_QR_RESULT, code, 0);
                    if (code == 0) {
                        m.obj = resp.optJSONObject("data");
                    }
                    handler.sendMessage(m);
                    if (code == 0 || code < 0) {
                        polling = false;
                    }
                }
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // 密码登录
    // ------------------------------------------------------------------

    private void doPasswordLogin() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString();
        if (username.length() == 0 || password.length() == 0) {
            Toast.makeText(this, R.string.toast_empty_input, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                JSONObject resp = api.passwordLogin(username, password);
                boolean ok = resp != null && resp.optInt("code", -1) == 0;
                Message m = handler.obtainMessage(MSG_LOGIN_RESULT, ok ? 1 : 0, 0);
                if (ok) {
                    m.obj = resp.optJSONObject("data");
                }
                handler.sendMessage(m);
            }
        }).start();
    }

    // ------------------------------------------------------------------
    // 登录成功处理
    // ------------------------------------------------------------------

    private void onLoginSuccess(JSONObject data) {
        if (data == null) {
            Toast.makeText(this, R.string.toast_login_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        String accessToken = data.optString("access_token", "");
        String refreshToken = data.optString("refresh_token", "");
        String mid = String.valueOf(data.opt("mid"));
        String uname = data.optString("uname", "");
        // csrf 参考版本从 cookie 中不易获取，这里先存空串
        account.saveLogin(accessToken, refreshToken, mid, "", uname);

        Toast.makeText(this, R.string.toast_login_ok, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    protected void onDestroy() {
        stopping = true;
        polling = false;
        super.onDestroy();
    }
}
