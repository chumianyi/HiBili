// Hi!Bili - RSA password encryption for legacy password login.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Security.Cryptography;
using System.Text;

namespace HiBili.WP.Helpers
{
    /// <summary>
    /// Encrypts the password for the legacy /oauth2/login flow.
    /// The Bilibili web endpoint returns an RSA public key (modulus + exponent)
    /// together with an MD5 salt ("hash"). The client must send
    ///   RSA_encrypt( hash + password )
    /// using PKCS#1 v1.5 padding.
    ///
    /// On Windows Phone Silverlight we use <see cref="RSACryptoServiceProvider"/>.
    /// </summary>
    public static class RsaHelper
    {
        /// <summary>
        /// Encrypt <paramref name="plain"/> with the given RSA public key, returning base64.
        /// </summary>
        /// <param name="modulusHex">Hex modulus from the API (often base64; caller decodes).</param>
        /// <param name="exponentHex">Hex exponent, usually "010001".</param>
        /// <param name="plain">UTF-8 plaintext (already prefixed with the MD5 salt).</param>
        public static string EncryptToBase64(byte[] modulus, byte[] exponent, string plain)
        {
            if (modulus == null) throw new ArgumentNullException("modulus");
            if (exponent == null) throw new ArgumentNullException("exponent");
            if (plain == null) plain = string.Empty;

            using (var rsa = new RSACryptoServiceProvider())
            {
                var parameters = new RSAParameters
                {
                    Modulus = modulus,
                    Exponent = exponent
                };
                rsa.ImportParameters(parameters);

                // Silverlight/Phone RSACryptoServiceProvider uses OAEP padding by default.
                // Bilibili expects PKCS#1 v1.5: pass false for fOAEP.
                byte[] plainBytes = Encoding.UTF8.GetBytes(plain);
                byte[] encrypted = rsa.Encrypt(plainBytes, false);
                return Convert.ToBase64String(encrypted);
            }
        }

        /// <summary>
        /// Convenience: base64-decode modulus and exponent then encrypt.
        /// </summary>
        public static string EncryptBase64Keys(string modulusBase64, string exponentBase64, string plain)
        {
            return EncryptToBase64(
                Convert.FromBase64String(modulusBase64),
                Convert.FromBase64String(exponentBase64),
                plain);
        }
    }
}
