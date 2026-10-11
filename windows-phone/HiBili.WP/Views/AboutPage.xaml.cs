// Hi!Bili - about page code-behind.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Windows;
using Microsoft.Phone.Controls;
using HiBili.WP.Services;

namespace HiBili.WP.Views
{
    /// <summary>
    /// About screen: version, GitHub, GPL note, attribution, logout.
    /// </summary>
    public partial class AboutPage : PhoneApplicationPage
    {
        public AboutPage()
        {
            InitializeComponent();
        }

        private async void LogoutButton_Click(object sender, RoutedEventArgs e)
        {
            MessageBoxResult confirm = MessageBox.Show(
                "确定要退出当前账号吗？",
                "退出登录",
                MessageBoxButton.OKCancel);

            if (confirm != MessageBoxResult.OK) return;

            try
            {
                // Best-effort server-side exit; local clear happens regardless.
                await BiliApiService.Instance.ExitLoginAsync();
            }
            catch { /* offline: still clear locally */ }

            AccountService.Instance.Clear();

            MessageBox.Show("已退出登录", "Hi！Bili", MessageBoxButton.OK);
            NavigationService.Navigate(new Uri("/Views/LoginPage.xaml", UriKind.Relative));
            NavigationService.RemoveBackEntry();
        }
    }
}
