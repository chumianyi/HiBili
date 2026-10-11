// Hi!Bili - MD5 helper (used by AppKey sign and WBI w_rid).
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System.Security.Cryptography;
using System.Text;

namespace HiBili.WP.Helpers
{
    /// <summary>
    /// Thin wrapper around <see cref="MD5"/> producing lowercase hex digests,
    /// matching what the Bilibili sign endpoints expect.
    /// </summary>
    public static class Md5Helper
    {
        /// <summary>UTF-8 MD5 hex digest (lowercase).</summary>
        public static string HexUtf8(string input)
        {
            if (input == null) input = string.Empty;
            byte[] data = Encoding.UTF8.GetBytes(input);
            return HashBytesToHex(MD5.Create().ComputeHash(data));
        }

        /// <summary>ASCII MD5 hex digest (lowercase) - used for legacy AppKey sign.</summary>
        public static string HexAscii(string input)
        {
            if (input == null) input = string.Empty;
            byte[] data = Encoding.ASCII.GetBytes(input);
            return HashBytesToHex(MD5.Create().ComputeHash(data));
        }

        private static string HashBytesToHex(byte[] bytes)
        {
            var sb = new StringBuilder(bytes.Length * 2);
            for (int i = 0; i < bytes.Length; i++)
            {
                sb.Append(bytes[i].ToString("x2"));
            }
            return sb.ToString();
        }
    }
}
