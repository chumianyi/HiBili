// Hi!Bili - login page code-behind.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Phone.Controls;
using HiBili.WP.Helpers;
using HiBili.WP.Models;
using HiBili.WP.Services;

namespace HiBili.WP.Views
{
    /// <summary>
    /// Login screen with two Pivot tabs:
    ///   - 扫码登录: render the TV QR and poll for confirmation.
    ///   - 密码登录: legacy username/password form.
    /// </summary>
    public partial class LoginPage : PhoneApplicationPage
    {
        private readonly DispatcherTimer _pollTimer = new DispatcherTimer();
        private string _currentAuthCode;
        private bool _busy;

        public LoginPage()
        {
            InitializeComponent();

            _pollTimer.Interval = TimeSpan.FromSeconds(2);
            _pollTimer.Tick += OnPollTick;
        }

        protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            LoadQrCodeAsync();
        }

        protected override void OnNavigatedFrom(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedFrom(e);
            _pollTimer.Stop();
        }

        // ----------------------------------------------------------------
        // QR login
        // ----------------------------------------------------------------

        private async void LoadQrCodeAsync()
        {
            if (_busy) return;
            _busy = true;
            QrStatusText.Text = "正在获取二维码…";

            try
            {
                var resp = await BiliApiService.Instance.GetTvAuthCodeAsync();
                if (resp == null || !resp.IsSuccess || resp.Data == null)
                {
                    QrStatusText.Text = "二维码获取失败，请重试";
                    return;
                }

                _currentAuthCode = resp.Data.AuthCode;
                QrCodeHelper.LoadInto(resp.Data.Url, QrImage, (ok, err) =>
                {
                    QrStatusText.Text = ok ? "请用哔哩哔哩手机客户端扫码" : "二维码渲染失败";
                });

                _pollTimer.Start();
            }
            catch (Exception ex)
            {
                QrStatusText.Text = "网络错误: " + ex.Message;
            }
            finally
            {
                _busy = false;
            }
        }

        private async void OnPollTick(object sender, EventArgs e)
        {
            if (string.IsNullOrEmpty(_currentAuthCode)) return;

            try
            {
                var resp = await BiliApiService.Instance.PollTvAuthCodeAsync(_currentAuthCode);
                if (resp == null) return;

                switch (resp.Code)
                {
                    case 0:
                        _pollTimer.Stop();
                        HandleLoginSuccess(resp.Data);
                        break;
                    case 86038:
                        QrStatusText.Text = "等待扫码…";
                        break;
                    case 86090:
                        QrStatusText.Text = "已扫码，请在手机上确认";
                        break;
                    case 86091:
                        _pollTimer.Stop();
                        QrStatusText.Text = "二维码已过期，请点击刷新";
                        break;
                    default:
                        QrStatusText.Text = resp.ErrorDisplay;
                        break;
                }
            }
            catch
            {
                // Transient network error; keep polling.
            }
        }

        private void HandleLoginSuccess(LoginResult result)
        {
            if (result == null) return;

            // Build a rough Cookie header from cookie_info.
            string cookieHeader = string.Empty;
            if (result.CookieInfo != null && result.CookieInfo.Cookies != null)
            {
                var parts = new System.Collections.Generic.List<string>();
                foreach (var c in result.CookieInfo.Cookies)
                {
                    parts.Add(c.Name + "=" + c.Value);
                }
                cookieHeader = string.Join("; ", parts);
            }

            AccountService.Instance.SaveLogin(result, cookieHeader);

            // Best-effort profile refresh.
            _ = RefreshProfileAsync();

            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                MessageBox.Show("登录成功，欢迎回来！", "Hi！Bili", MessageBoxButton.OK);
                NavigationService.Navigate(new Uri("/Views/MainPage.xaml", UriKind.Relative));
                NavigationService.RemoveBackEntry();
            });
        }

        private async System.Threading.Tasks.Task RefreshProfileAsync()
        {
            try
            {
                var info = await BiliApiService.Instance.GetNavInfoAsync();
                if (info != null) AccountService.Instance.SaveProfile(info);
            }
            catch { /* ignore */ }
        }

        private void RefreshQr_Click(object sender, RoutedEventArgs e)
        {
            _pollTimer.Stop();
            LoadQrCodeAsync();
        }

        // ----------------------------------------------------------------
        // Password login
        // ----------------------------------------------------------------

        private async void LoginButton_Click(object sender, RoutedEventArgs e)
        {
            PasswordErrorText.Text = string.Empty;

            if (string.IsNullOrWhiteSpace(UsernameBox.Text) ||
                string.IsNullOrWhiteSpace(PasswordBox.Password))
            {
                PasswordErrorText.Text = "请输入账号和密码";
                return;
            }

            LoginButton.IsEnabled = false;
            try
            {
                var resp = await BiliApiService.Instance.LoginByPasswordAsync(
                    UsernameBox.Text.Trim(), PasswordBox.Password);

                if (resp != null && resp.IsSuccess && resp.Data != null)
                {
                    HandleLoginSuccess(resp.Data);
                }
                else
                {
                    PasswordErrorText.Text = resp != null ? resp.ErrorDisplay : "登录失败";
                }
            }
            catch (Exception ex)
            {
                PasswordErrorText.Text = "网络错误: " + ex.Message;
            }
            finally
            {
                LoginButton.IsEnabled = true;
            }
        }
    }
}
