// Hi!Bili - home page code-behind.
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
    /// Panorama home: shows the signed-in nickname and exposes the About screen.
    /// All three hubs are intentional placeholders for this reference build.
    /// </summary>
    public partial class MainPage : PhoneApplicationPage
    {
        public MainPage()
        {
            InitializeComponent();
        }

        protected override async void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            BindIdentity();

            // Best-effort refresh; ignore failures (offline / expired token).
            try
            {
                var info = await BiliApiService.Instance.GetNavInfoAsync();
                if (info != null)
                {
                    AccountService.Instance.SaveProfile(info);
                }
            }
            catch { /* offline */ }
            BindIdentity();
        }

        private void BindIdentity()
        {
            string name = AccountService.Instance.UserName;
            if (string.IsNullOrEmpty(name))
            {
                name = "Bilibili 用户";
            }
            GreetingText.Text = "你好，" + name;
            ProfileText.Text = "昵称：" + name + "\nUID：" + AccountService.Instance.Mid;
        }

        private void AboutButton_Click(object sender, EventArgs e)
        {
            NavigationService.Navigate(new Uri("/Views/AboutPage.xaml", UriKind.Relative));
        }
    }
}
