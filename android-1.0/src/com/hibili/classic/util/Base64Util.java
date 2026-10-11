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

/**
 * 手写 Base64 编解码。
 * 说明：android.util.Base64 自 API 8 才提供，而本参考版本 minSdk=1，
 * 因此这里用纯 Java 实现一份最基础的实现，供 RSA 与二维码场景使用。
 */
public final class Base64Util {

    private static final char[] ENCODE_TABLE =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private static final int[] DECODE_TABLE = new int[128];

    static {
        for (int i = 0; i < DECODE_TABLE.length; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            DECODE_TABLE[ENCODE_TABLE[i]] = i;
        }
    }

    private Base64Util() {
    }

    public static String encode(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = data.length;
        while (i < len) {
            int b0 = data[i++] & 0xff;
            sb.append(ENCODE_TABLE[b0 >>> 2]);
            if (i >= len) {
                sb.append(ENCODE_TABLE[(b0 & 0x03) << 4]);
                sb.append("==");
                break;
            }
            int b1 = data[i++] & 0xff;
            if (i >= len) {
                sb.append(ENCODE_TABLE[((b0 & 0x03) << 4) | ((b1 & 0xf0) >>> 4)]);
                sb.append(ENCODE_TABLE[(b1 & 0x0f) << 2]);
                sb.append("=");
                break;
            }
            int b2 = data[i++] & 0xff;
            sb.append(ENCODE_TABLE[((b0 & 0x03) << 4) | ((b1 & 0xf0) >>> 4)]);
            sb.append(ENCODE_TABLE[((b1 & 0x0f) << 2) | ((b2 & 0xc0) >>> 6)]);
            sb.append(ENCODE_TABLE[b2 & 0x3f]);
        }
        return sb.toString();
    }

    public static byte[] decode(String base64) {
        if (base64 == null) {
            return new byte[0];
        }
        String s = base64.trim();
        // 去除空白与换行
        StringBuilder clean = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=' || (c < 128 && DECODE_TABLE[c] != -1)) {
                clean.append(c);
            }
        }
        s = clean.toString();
        int pad = 0;
        for (int i = s.length() - 1; i >= 0 && s.charAt(i) == '='; i--) {
            pad++;
        }
        int outLen = (s.length() * 3) / 4 - pad;
        byte[] out = new byte[outLen];
        int bits = 0;
        int bitCount = 0;
        int index = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') {
                break;
            }
            int v = DECODE_TABLE[c];
            if (v < 0) {
                continue;
            }
            bits = (bits << 6) | v;
            bitCount += 6;
            if (bitCount >= 8) {
                bitCount -= 8;
                out[index++] = (byte) ((bits >> bitCount) & 0xff);
            }
        }
        return out;
    }
}
