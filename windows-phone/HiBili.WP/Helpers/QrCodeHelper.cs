// Hi!Bili - QR code rendering helper.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System;
using System.Net;
using System.Windows.Media.Imaging;

namespace HiBili.WP.Helpers
{
    /// <summary>
    /// Renders the TV-login URL into a QR code image.
    ///
    /// Embedding a full QR encoder (e.g. ZXing) on Windows Phone Silverlight
    /// would pull in a large third-party assembly. For this reference build we
    /// delegate rendering to the public goqr.me image API, which returns a PNG
    /// we can hand straight to an <see cref="System.Windows.Controls.Image"/>.
    ///
    /// To go offline, swap <see cref="BuildQrImageUri"/> for a local ZXing
    /// render (the URL passed in is already the value that must be encoded).
    /// </summary>
    public static class QrCodeHelper
    {
        /// <summary>Size of the rendered QR bitmap, pixels.</summary>
        public const int QrSize = 360;

        /// <summary>Build the goqr.me URI for the given payload URL.</summary>
        public static Uri BuildQrImageUri(string payloadUrl)
        {
            if (string.IsNullOrEmpty(payloadUrl))
            {
                throw new ArgumentException("payloadUrl is empty", "payloadUrl");
            }

            string escaped = Uri.EscapeDataString(payloadUrl);
            return new Uri(
                string.Format(
                    "https://api.qrserver.com/v1/create-qr-code/?size={0}x{0}&margin=8&data={1}",
                    QrSize,
                    escaped),
                UriKind.Absolute);
        }

        /// <summary>
        /// Download the QR bitmap asynchronously and hand it to a target Image control.
        /// Errors are swallowed and surfaced via the callback so the page can show a retry.
        /// </summary>
        public static void LoadInto(
            string payloadUrl,
            System.Windows.Controls.Image target,
            Action<bool /*success*/, Exception /*error*/> completed)
        {
            if (target == null) throw new ArgumentNullException("target");

            BitmapImage bmp = new BitmapImage();
            try
            {
                bmp.UriSource = BuildQrImageUri(payloadUrl);
                bmp.CreateOptions = BitmapCreateOptions.None;
                target.Source = bmp;
                if (completed != null) completed(true, null);
            }
            catch (Exception ex)
            {
                if (completed != null) completed(false, ex);
            }
        }
    }
}
