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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * RSA 加密工具（Android 1.0 内置 javax.crypto）。
 * 用于密码登录前对密码 / 手机号进行加密。
 * Base64 使用本包手写实现（android.util.Base64 在 API 8 才提供）。
 */
public final class RsaUtil {

    private RsaUtil() {
    }

    /**
     * 用 B 站登录接口下发的 RSA 公钥（模数 + 指数拼接后的 X.509 Base64 字符串）加密明文。
     *
     * @param publicKeyBase64 X.509 编码的公钥 Base64
     * @param plainText       明文（UTF-8）
     * @return Base64 密文；失败返回空串
     */
    public static String encrypt(String publicKeyBase64, String plainText) {
        try {
            byte[] keyBytes = Base64Util.decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pubKey = kf.generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64Util.encode(encrypted);
        } catch (Exception e) {
            return "";
        }
    }
}
