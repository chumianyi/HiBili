// Hi!Bili - generic Bilibili API envelope.
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2024 HiBili Team
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

using System.Runtime.Serialization;

namespace HiBili.WP.Models
{
    /// <summary>
    /// Standard Bilibili API envelope: { code, message/msg, data }.
    /// </summary>
    [DataContract]
    public class ApiResponse<T>
    {
        /// <summary>Business code. 0 means success.</summary>
        [DataMember(Name = "code")]
        public int Code { get; set; }

        /// <summary>Human readable message (web endpoints use "message", TV endpoints use "message").</summary>
        [DataMember(Name = "message")]
        public string Message { get; set; }

        /// <summary>Alias for <see cref="Message"/> on some endpoints.</summary>
        [DataMember(Name = "msg")]
        public string Msg { get; set; }

        /// <summary>Payload.</summary>
        [DataMember(Name = "data")]
        public T Data { get; set; }

        public bool IsSuccess
        {
            get { return Code == 0; }
        }

        public string ErrorDisplay
        {
            get { return !string.IsNullOrEmpty(Message) ? Message : (Msg ?? ("Error code " + Code)); }
        }
    }

    /// <summary>Non-generic envelope for endpoints that return no data payload.</summary>
    [DataContract]
    public class ApiResponse
    {
        [DataMember(Name = "code")]
        public int Code { get; set; }

        [DataMember(Name = "message")]
        public string Message { get; set; }

        [DataMember(Name = "msg")]
        public string Msg { get; set; }

        public bool IsSuccess
        {
            get { return Code == 0; }
        }
    }
}
