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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 工具类（Android 1.0 内置 java.security.MessageDigest 即可）。
 * 用于 B 站 appkey 签名。
 */
public final class Md5Util {

    private Md5Util() {
    }

    /** 返回小写十六进制 MD5。 */
    public static String md5(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // 不可能发生
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            if (b < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }
}
