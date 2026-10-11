/*
 * Hi！Bili - WebOS Reference Build
 * Copyright (C) 2024 HiBili Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * MainView — user info feed with real prototype content:
 *   - dynamic cards (sample Bilibili-style dynamics)
 *   - video cards (sample recommended videos)
 *   - favorites grid
 *   - watch history list
 */
var MainView = (function () {

  // ---- Static prototype data ----
  var SAMPLE_DYNAMICS = [
    {
      name: '哔哩哔哩弹幕网',
      avatar: 'B',
      time: '2 小时前',
      text: '【2024 BML】即将开票！今年我们来到了上海体育场，准备好和我们一起狂欢了吗？',
      likes: '1.2万',
      comments: '864'
    },
    {
      name: '老番茄',
      avatar: '番',
      time: '5 小时前',
      text: '新视频《我在游戏里开了一家公司》上线了！这次是模拟经营题材，做了整整三个月，希望大家喜欢。',
      likes: '8.6万',
      comments: '1.2万'
    },
    {
      name: '央视新闻',
      avatar: '新',
      time: '昨天',
      text: '我国成功发射遥感四十号卫星，卫星顺利进入预定轨道，发射任务获得圆满成功。',
      likes: '5.4万',
      comments: '3200'
    }
  ];

  var SAMPLE_VIDEOS = [
    { title: '【4K】沉浸式漫步杭州西湖，秋日的江南有多美', up: '旅行日记', views: '234.5万', duration: '12:34', cover: '西湖' },
    { title: '我用100天学完了计算机科学全部课程', up: '硬核阿婆', views: '89.2万', duration: '18:02', cover: 'CS' },
    { title: '【料理】在家复刻B站爆款红烧肉，肥而不腻', up: '厨房小哥', views: '156.7万', duration: '08:45', cover: '红烧肉' },
    { title: '钢琴改编《晴天》——献给所有错过的人', up: '琴键上的猫', views: '442.1万', duration: '04:18', cover: '晴天' }
  ];

  var SAMPLE_FAVORITES = [
    { name: '日语五十音图入门', count: 12 },
    { name: 'Python 数据分析教程', count: 36 },
    { name: '设计模式精讲', count: 8 },
    { name: '经典动画合集', count: 24 }
  ];

  var SAMPLE_HISTORY = [
    { title: '【直播回放】2024 年总决赛 G5 最后五分钟', up: '体育赛事', time: '3 小时前' },
    { title: '为什么猫喜欢纸箱子？动物学家解释了', up: '好奇宝宝', time: '昨天' },
    { title: '手作 | 用环氧树脂做一盏星空灯', up: '手工美学', time: '2 天前' }
  ];

  function render(container, callbacks) {
    BiliApi.getUserInfo(function (err, data) {
      var uname = 'B 站用户';
      var level = 0;
      var face = '';
      if (!err && data && data.isLogin) {
        uname = data.uname || uname;
        level = data.level_info ? data.level_info.current_level : 0;
        face = data.face || '';
      } else if (!AccountManager.isLoggedIn()) {
        callbacks.onLoggedOut();
        return;
      }

      container.innerHTML =
        '<div class="hibili-main">' +
          '<div class="main-topbar">' +
            '<div class="topbar-title">Hi！Bili</div>' +
            '<div class="topbar-user">' + escapeHtml(uname) + '</div>' +
            '<button class="btn-ghost" id="btnAbout">关于</button>' +
          '</div>' +

          '<div class="user-card">' +
            (face
              ? '<img src="' + face + '" class="user-face" alt="">'
              : '<div class="user-face user-face-fallback">' + escapeHtml(uname.charAt(0)) + '</div>') +
            '<div class="user-info">' +
              '<div class="user-name">' + escapeHtml(uname) + '</div>' +
              '<div class="user-sign">LV.' + level + ' · 欢迎回来</div>' +
            '</div>' +
          '</div>' +

          '<div class="section-title">最新动态</div>' +
          renderDynamics() +

          '<div class="section-title">为你推荐</div>' +
          renderVideos() +

          '<div class="section-title">我的收藏</div>' +
          renderFavorites() +

          '<div class="section-title">最近观看</div>' +
          renderHistory() +

          '<div class="bottom-nav">' +
            '<div class="nav-item active">首页</div>' +
            '<div class="nav-item">动态</div>' +
            '<div class="nav-item">消息</div>' +
            '<div class="nav-item">我的</div>' +
          '</div>' +
        '</div>';

      document.getElementById('btnAbout').addEventListener('click', function () {
        callbacks.onAbout();
      });
    });
  }

  function renderDynamics() {
    var html = '<div class="dynamic-list">';
    SAMPLE_DYNAMICS.forEach(function (d) {
      html +=
        '<div class="dynamic-card">' +
          '<div class="dynamic-head">' +
            '<div class="dynamic-avatar">' + escapeHtml(d.avatar) + '</div>' +
            '<div class="dynamic-head-info">' +
              '<div class="dynamic-name">' + escapeHtml(d.name) + '</div>' +
              '<div class="dynamic-time">' + escapeHtml(d.time) + '</div>' +
            '</div>' +
          '</div>' +
          '<div class="dynamic-text">' + escapeHtml(d.text) + '</div>' +
          '<div class="dynamic-meta">' +
            '<span>赞 ' + escapeHtml(d.likes) + '</span>' +
            '<span>评论 ' + escapeHtml(d.comments) + '</span>' +
          '</div>' +
        '</div>';
    });
    return html + '</div>';
  }

  function renderVideos() {
    var html = '<div class="video-grid">';
    SAMPLE_VIDEOS.forEach(function (v) {
      html +=
        '<div class="video-card">' +
          '<div class="video-cover">' + escapeHtml(v.cover) +
            '<span class="video-duration">' + escapeHtml(v.duration) + '</span>' +
          '</div>' +
          '<div class="video-title">' + escapeHtml(v.title) + '</div>' +
          '<div class="video-meta">' + escapeHtml(v.up) + ' · 播放 ' + escapeHtml(v.views) + '</div>' +
        '</div>';
    });
    return html + '</div>';
  }

  function renderFavorites() {
    var html = '<div class="fav-grid">';
    SAMPLE_FAVORITES.forEach(function (f) {
      html +=
        '<div class="fav-card">' +
          '<div class="fav-icon">藏</div>' +
          '<div class="fav-name">' + escapeHtml(f.name) + '</div>' +
          '<div class="fav-count">' + f.count + ' 个内容</div>' +
        '</div>';
    });
    return html + '</div>';
  }

  function renderHistory() {
    var html = '<div class="history-list">';
    SAMPLE_HISTORY.forEach(function (h) {
      html +=
        '<div class="history-item">' +
          '<div class="history-cover">看</div>' +
          '<div class="history-info">' +
            '<div class="history-title">' + escapeHtml(h.title) + '</div>' +
            '<div class="history-meta">' + escapeHtml(h.up) + ' · ' + escapeHtml(h.time) + '</div>' +
          '</div>' +
        '</div>';
    });
    return html + '</div>';
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  return { render: render };
})();
