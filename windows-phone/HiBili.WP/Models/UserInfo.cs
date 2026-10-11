// Hi!Bili - logged-in user profile model.
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
    /// Subset of the /x/web-interface/nav "data" object.
    /// Enough to greet the user on the home page.
    /// </summary>
    [DataContract]
    public class UserInfo
    {
        [DataMember(Name = "mid")]
        public long Mid { get; set; }

        [DataMember(Name = "uname")]
        public string Name { get; set; }

        [DataMember(Name = "face")]
        public string Face { get; set; }

        [DataMember(Name = "isLogin")]
        public bool IsLogin { get; set; }

        [DataMember(Name = "vipStatus")]
        public int VipStatus { get; set; }

        [DataMember(Name = "vipDueDate")]
        public long VipDueDate { get; set; }

        [DataMember(Name = "level_info")]
        public LevelInfo Level { get; set; }

        public string DisplayName
        {
            get { return string.IsNullOrEmpty(Name) ? "Bilibili User" : Name; }
        }
    }

    [DataContract]
    public class LevelInfo
    {
        [DataMember(Name = "current_level")]
        public int CurrentLevel { get; set; }
    }
}
