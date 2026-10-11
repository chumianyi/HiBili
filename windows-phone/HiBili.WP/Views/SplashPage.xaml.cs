// Hi!Bili - splash screen code-behind.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System.Windows.Threading;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using HiBili.WP.Services;

namespace HiBili.WP.Views
{
    /// <summary>
    /// Shows the brand for ~2 seconds, then routes to login or home.
    /// </summary>
    public partial class SplashPage : PhoneApplicationPage
    {
        private readonly DispatcherTimer _timer = new DispatcherTimer();

        public SplashPage()
        {
            InitializeComponent();

            // Hide the system tray for a clean splash.
            SystemTray.IsVisible = false;

            _timer.Interval = System.TimeSpan.FromSeconds(2);
            _timer.Tick += (s, e) =>
            {
                _timer.Stop();
                NavigateNext();
            };
        }

        protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            _timer.Start();
        }

        protected override void OnNavigatedFrom(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            _timer.Stop();
            SystemTray.IsVisible = true;
        }

        private void NavigateNext()
        {
            if (AccountService.Instance.HasSession)
            {
                NavigationService.Navigate(new System.Uri("/Views/MainPage.xaml", System.UriKind.Relative));
            }
            else
            {
                NavigationService.Navigate(new System.Uri("/Views/LoginPage.xaml", System.UriKind.Relative));
            }
            NavigationService.RemoveBackEntry();
        }
    }
}
