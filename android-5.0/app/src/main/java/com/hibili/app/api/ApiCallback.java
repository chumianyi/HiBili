/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.api;

/**
 * Generic async API callback. {@code onSuccess} is invoked on the thread that
 * ran the request (network thread); callers that touch the UI must post to
 * the main thread themselves.
 */
public interface ApiCallback<T> {
    void onSuccess(T data);
    void onError(int code, String message);
}
