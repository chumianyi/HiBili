/*
 * This file is part of Hi！Bili
 * Copyright (C) 2026 chumianyi
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.hibili.app.model;

/** Wraps code/message/data of a Bilibili JSON envelope. */
public class DynamicResult<T> {
    public int code;
    public String message;
    public T data;
}
