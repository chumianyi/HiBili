# This file is part of Hi！Bili
# Copyright (C) 2026 chumianyi
# SPDX-License-Identifier: GPL-3.0-or-later

-keepattributes Signature
-keepattributes *Annotation*

# Gson model classes
-keep class com.hibili.app.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# zxing
-dontwarn com.google.zxing.**
