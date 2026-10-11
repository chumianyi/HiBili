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

import com.hibili.classic.HiBiliApp;
import com.hibili.classic.R;

/**
 * 启动页：停留约 1.5 秒后跳转主页或登录页。
 */
public class SplashActivity extends Activity {

    private final Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        handler.postDelayed(new Runnable() {
            public void run() {
                Intent intent;
                if (HiBiliApp.getAccount().isLoggedIn()) {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, 1500);
    }
}
