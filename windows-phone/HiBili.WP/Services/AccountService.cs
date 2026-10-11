// Hi!Bili - credential storage in isolated storage.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.IO.IsolatedStorage;
using HiBili.WP.Models;

namespace HiBili.WP.Services
{
    /// <summary>
    /// Persists the TV-login token set in <see cref="IsolatedStorageSettings"/>.
    /// This is the same per-app store every WP Silverlight app gets; it is
    /// sandboxed and cleared on uninstall.
    ///
    /// NOTE: This is a reference/prototype build. A production client would
    /// move the access token into the ProtectedData / credential locker.
    /// </summary>
    public class AccountService
    {
        private const string KeyAccessToken = "hibili.access_token";
        private const string KeyRefreshToken = "hibili.refresh_token";
        private const string KeyMid = "hibili.mid";
        private const string KeyCookie = "hibili.cookie";
        private const string KeyUserName = "hibili.uname";
        private const string KeyFace = "hibili.face";

        private static readonly Lazy<AccountService> _instance =
            new Lazy<AccountService>(() => new AccountService());

        public static AccountService Instance
        {
            get { return _instance.Value; }
        }

        private IsolatedStorageSettings Settings
        {
            get { return IsolatedStorageSettings.ApplicationSettings; }
        }

        public bool HasSession
        {
            get { return !string.IsNullOrEmpty(AccessToken); }
        }

        public string AccessToken
        {
            get { return GetString(KeyAccessToken); }
        }

        public string RefreshToken
        {
            get { return GetString(KeyRefreshToken); }
        }

        public long Mid
        {
            get { return GetLong(KeyMid); }
        }

        public string Cookie
        {
            get { return GetString(KeyCookie); }
        }

        public string UserName
        {
            get { return GetString(KeyUserName); }
        }

        public string Face
        {
            get { return GetString(KeyFace); }
        }

        /// <summary>Persist a successful TV-login result plus the parsed cookie header.</summary>
        public void SaveLogin(LoginResult result, string cookieHeader)
        {
            if (result == null) throw new ArgumentNullException("result");

            SetValue(KeyAccessToken, result.AccessToken ?? string.Empty);
            SetValue(KeyRefreshToken, result.RefreshToken ?? string.Empty);
            SetValue(KeyMid, result.Mid);
            SetValue(KeyCookie, cookieHeader ?? string.Empty);

            // Best-effort display name from cookie_info is not available here;
            // the caller refreshes profile via BiliApiService.GetNavInfoAsync.
            Save();
        }

        /// <summary>Persist the profile fields returned by /x/web-interface/nav.</summary>
        public void SaveProfile(UserInfo info)
        {
            if (info == null) return;
            SetValue(KeyUserName, info.Name ?? string.Empty);
            SetValue(KeyFace, info.Face ?? string.Empty);
            SetValue(KeyMid, info.Mid);
            Save();
        }

        /// <summary>Wipe every credential (called on logout).</summary>
        public void Clear()
        {
            Remove(KeyAccessToken);
            Remove(KeyRefreshToken);
            Remove(KeyMid);
            Remove(KeyCookie);
            Remove(KeyUserName);
            Remove(KeyFace);
            Save();
        }

        private string GetString(string key)
        {
            object v;
            return Settings.TryGetValue(key, out v) ? (v as string ?? string.Empty) : string.Empty;
        }

        private long GetLong(string key)
        {
            object v;
            long l;
            if (Settings.TryGetValue(key, out v) && long.TryParse(v.ToString(), out l)) return l;
            return 0;
        }

        private void SetValue(string key, object value)
        {
            if (Settings.Contains(key)) Settings[key] = value;
            else Settings.Add(key, value);
        }

        private void Remove(string key)
        {
            if (Settings.Contains(key)) Settings.Remove(key);
        }

        private void Save()
        {
            try { Settings.Save(); }
            catch { /* isolated storage full / locked: ignore on prototype */ }
        }
    }
}
