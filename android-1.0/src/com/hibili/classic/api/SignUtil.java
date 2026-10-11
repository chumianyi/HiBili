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
package com.hibili.classic.api;

import com.hibili.classic.util.Md5Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * B 站接口签名工具。
 *
 * appkey 签名（TV 登录等接口）：
 *   1. 收集所有业务参数（含 appkey）
 *   2. 按 key 字典序排序，拼接成 key=value&key=value
 *   3. 末尾拼接 appsec
 *   4. md5 得到 sign
 *
 * WBI 签名：参考版本做简化处理——由于 WBI 需要先请求 nav 接口获取
 * img_key / sub_key 再做混淆重排，逻辑较重；这里保留方法名，
 * 内部直接退化为 appkey 签名，便于后续扩展。
 */
public final class SignUtil {

    public static final String APPKEY = "dfca71928277209b";
    public static final String APPSEC = "b5475a8825547a4fc26c7d518eaaa02e";

    private SignUtil() {
    }

    /**
     * 为参数表附加 appkey 与 sign，返回新的已签名参数表。
     */
    public static TreeMap signWithAppkey(Map params) {
        TreeMap out = new TreeMap();
        if (params != null) {
            for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry) it.next();
                out.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        out.put("appkey", APPKEY);
        out.put("sign", md5Sign(out));
        return out;
    }

    /**
     * 计算 appkey 签名值（不修改入参）。
     */
    public static String md5Sign(Map params) {
        List keys = new ArrayList(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = String.valueOf(keys.get(i));
            if ("sign".equals(key)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(String.valueOf(params.get(key)));
        }
        sb.append(APPSEC);
        return Md5Util.md5(sb.toString());
    }

    /**
     * WBI 签名（简化）：参考版本暂退化为 appkey 签名。
     * TODO: 后续版本实现 img_key/sub_key 混淆重排。
     */
    public static TreeMap signWithWbi(Map params, String imgKey, String subKey) {
        // 简化实现：直接复用 appkey 签名
        return signWithAppkey(params);
    }
}
