// Hi!Bili - TV QR-code login result model.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System.Runtime.Serialization;

namespace HiBili.WP.Models
{
    /// <summary>
    /// Response of POST /x/passport-tv-login/qrcode/auth_code.
    /// </summary>
    [DataContract]
    public class QrCodeAuthData
    {
        /// <summary>Opaque auth code to poll with.</summary>
        [DataMember(Name = "auth_code")]
        public string AuthCode { get; set; }

        /// <summary>URL that must be rendered into a QR code for the phone app to scan.</summary>
        [DataMember(Name = "url")]
        public string Url { get; set; }
    }

    /// <summary>
    /// Response of POST /x/passport-tv-login/qrcode/poll.
    /// On success carries the access token set.
    /// </summary>
    [DataContract]
    public class LoginResult
    {
        [DataMember(Name = "access_token")]
        public string AccessToken { get; set; }

        [DataMember(Name = "refresh_token")]
        public string RefreshToken { get; set; }

        [DataMember(Name = "mid")]
        public long Mid { get; set; }

        [DataMember(Name = "expires_in")]
        public long ExpiresIn { get; set; }

        [DataMember(Name = "cookie_info")]
        public CookieInfo CookieInfo { get; set; }
    }

    [DataContract]
    public class CookieInfo
    {
        [DataMember(Name = "cookies")]
        public CookieItem[] Cookies { get; set; }
    }

    [DataContract]
    public class CookieItem
    {
        [DataMember(Name = "name")]
        public string Name { get; set; }

        [DataMember(Name = "value")]
        public string Value { get; set; }

        [DataMember(Name = "http_only")]
        public int HttpOnly { get; set; }

        [DataMember(Name = "expires")]
        public long Expires { get; set; }
    }
}
