/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.util;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;

/**
 * RSA-OAEP / RSA/ECB/PKCS1 cipher used by Bilibili password login.
 *
 * Server returns a base64-encoded X.509 SubjectPublicKeyInfo (PEM stripped)
 * under /x/passport-login/web/key. We encrypt "salt + password" with it
 * and base64 the result.
 */
public final class RsaUtil {

    private RsaUtil() {}

    public static String encryptBase64(String pubKeyBase64, String plain) throws Exception {
        byte[] keyBytes = Base64.decode(pubKeyBase64, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        byte[] out = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }
}
