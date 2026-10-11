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
package com.hibili.classic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * 基于 java.net.HttpURLConnection 的极简 HTTP 工具。
 * 不依赖 OkHttp，兼容 Android 1.0。
 *
 * 已知限制：现代 B 站接口多为 HTTPS，Android 1.0 设备的 TLS 栈可能
 * 无法协商现代 TLS 版本，实际请求可能失败；本类只负责发起请求。
 */
public final class HttpUtil {

    public static final String UA =
            "Mozilla/5.0 (Linux; U; Android 1.0; en-us) AppleWebKit/525.10 (KHTML, like Gecko) Version/3.0.4 Mobile/9B176 Safari/525.10";

    private HttpUtil() {
    }

    /** GET 请求，返回响应体字符串。失败返回 null。 */
    public static String get(String urlStr, Map headers) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", UA);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            applyHeaders(conn, headers);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400)
                    ? conn.getInputStream() : conn.getErrorStream();
            return readAll(is);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** POST 表单请求，body 为 application/x-www-form-urlencoded。失败返回 null。 */
    public static String postForm(String urlStr, Map params, Map headers) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            applyHeaders(conn, headers);

            String body = encodeParams(params);
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400)
                    ? conn.getInputStream() : conn.getErrorStream();
            return readAll(is);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void applyHeaders(HttpURLConnection conn, Map headers) {
        if (headers == null) {
            return;
        }
        for (java.util.Iterator it = headers.entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            conn.setRequestProperty(String.valueOf(e.getKey()),
                    String.valueOf(e.getValue()));
        }
    }

    private static String encodeParams(Map params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Iterator it = params.entrySet().iterator(); it.hasNext();) {
            Map.Entry e = (Map.Entry) it.next();
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode(String.valueOf(e.getKey()), "UTF-8"))
                        .append('=')
                        .append(URLEncoder.encode(String.valueOf(e.getValue()), "UTF-8"));
            } catch (Exception ex) {
                // 跳过编码失败的参数
            }
        }
        return sb.toString();
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }
}
