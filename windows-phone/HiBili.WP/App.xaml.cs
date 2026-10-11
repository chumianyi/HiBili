// Hi!Bili - application code-behind.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Diagnostics;
using System.Windows;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;

namespace HiBili.WP
{
    /// <summary>
    /// Localization-aware application object.
    /// </summary>
    public partial class App : Application
    {
        /// <summary>Fast access to the root frame of the Phone Application.</summary>
        public static PhoneApplicationFrame RootFrame { get; private set; }

        /// <summary>Constructor: build the UI, wire up unhandled exceptions.</summary>
        public App()
        {
            // Global handler for uncaught exceptions.
            UnhandledException += Application_UnhandledException;

            InitializeComponent();

            // Standard phone Silverlight initialization
            if (Debugger.IsAttached)
            {
                // Display the current frame rate counters.
                Application.Current.Host.Settings.EnableFrameRateCounter = true;
                PhoneApplicationService.Current.UserIdleDetectionMode = IdleDetectionMode.Disabled;
            }

            // Create the frame but don't set it as RootVisual yet; this lets the splash
            // screen remain active until the application finishes running.
            RootFrame = new PhoneApplicationFrame();
            RootFrame.Navigated += CompleteInitializePhoneApplication;

            // Handle navigation failures.
            RootFrame.NavigationFailed += RootFrame_NavigationFailed;

            // Ensure we're not in the dormant state.
            if (PhoneApplicationService.Current.StartupMode == StartupMode.Activate)
            {
                // Restored from dormant state - nothing extra to do here.
            }

            RootVisual = RootFrame;
        }

        // Code to execute when the application is launching (e.g. from Start)
        // This code will not execute when the application is reactivated
        private void Application_Launching(object sender, LaunchingEventArgs e) { }

        // Code to execute when the application is activated (brought to foreground)
        // This code will not execute when the application is first launched
        private void Application_Activated(object sender, ActivatedEventArgs e) { }

        // Code to execute when the application is deactivated (sent to background)
        // This code will not execute when the application is closing
        private void Application_Deactivated(object sender, DeactivatedEventArgs e) { }

        // Code to execute when the application is closing (e.g. user pressed Back)
        // This code will not execute when the application is deactivated
        private void Application_Closing(object sender, ClosingEventArgs e) { }

        private void RootFrame_NavigationFailed(object sender, NavigationFailedEventArgs e)
        {
            if (Debugger.IsAttached)
            {
                // A navigation has failed; break into the debugger.
                Debugger.Break();
            }
        }

        private void Application_UnhandledException(object sender, ApplicationUnhandledExceptionEventArgs e)
        {
            if (Debugger.IsAttached)
            {
                Debugger.Break();
            }
        }

        private void CompleteInitializePhoneApplication(object sender, NavigationEventArgs e)
        {
            if (RootVisual != RootFrame)
            {
                RootVisual = RootFrame;
            }
            RootFrame.Navigated -= CompleteInitializePhoneApplication;
        }
    }
}
