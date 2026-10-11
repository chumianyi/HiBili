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
package com.hibili.classic.account;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 账户凭证存储。使用 SharedPreferences（API 1 可用）。
 * 存储 access_token / refresh_token / mid / csrf / uname。
 */
public final class AccountManager {

    private static final String PREF_NAME = "hibili_account";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_MID = "mid";
    private static final String KEY_CSRF = "csrf";
    private static final String KEY_UNAME = "uname";

    private static AccountManager instance;

    private final SharedPreferences sp;

    private AccountManager(Context ctx) {
        this.sp = ctx.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AccountManager get(Context ctx) {
        if (instance == null) {
            instance = new AccountManager(ctx);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        String token = sp.getString(KEY_ACCESS_TOKEN, "");
        return token != null && token.length() > 0;
    }

    public String getAccessToken() {
        return sp.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return sp.getString(KEY_REFRESH_TOKEN, "");
    }

    public String getMid() {
        return sp.getString(KEY_MID, "");
    }

    public String getCsrf() {
        return sp.getString(KEY_CSRF, "");
    }

    public String getUname() {
        return sp.getString(KEY_UNAME, "");
    }

    public void saveLogin(String accessToken, String refreshToken,
                          String mid, String csrf, String uname) {
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(KEY_ACCESS_TOKEN, accessToken == null ? "" : accessToken);
        ed.putString(KEY_REFRESH_TOKEN, refreshToken == null ? "" : refreshToken);
        ed.putString(KEY_MID, mid == null ? "" : mid);
        ed.putString(KEY_CSRF, csrf == null ? "" : csrf);
        ed.putString(KEY_UNAME, uname == null ? "" : uname);
        ed.commit();
    }

    public void clear() {
        SharedPreferences.Editor ed = sp.edit();
        ed.clear();
        ed.commit();
    }
}
