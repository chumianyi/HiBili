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
package com.hibili.classic;

import android.app.Application;
import android.content.Context;

import com.hibili.classic.account.AccountManager;
import com.hibili.classic.api.BiliApi;

/**
 * 全局 Application：持有账户与 API 单例。
 */
public class HiBiliApp extends Application {

    private static Context appContext;
    private static AccountManager accountManager;
    private static BiliApi biliApi;

    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        accountManager = AccountManager.get(this);
        biliApi = new BiliApi(accountManager);
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static AccountManager getAccount() {
        return accountManager;
    }

    public static BiliApi getApi() {
        return biliApi;
    }
}
