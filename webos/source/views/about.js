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

/* AboutView — icon, version, links, acknowledgements, logout. */
var AboutView = (function () {
  function render(container, callbacks) {
    container.innerHTML =
      '<div class="hibili-about">' +
        '<div class="about-top">' +
          '<img src="assets/icon-128.png" class="about-icon" alt="">' +
          '<div class="about-name">Hi！Bili</div>' +
          '<div class="about-version">WebOS Reference v1.0.0</div>' +
        '</div>' +

        '<div class="about-section">' +
          '<div class="about-row"><div class="about-key">项目主页</div><div class="about-val">github.com/chumianyi/HiBili</div></div>' +
          '<div class="about-row"><div class="about-key">开源协议</div><div class="about-val">GPL v3.0</div></div>' +
          '<div class="about-row"><div class="about-key">技术栈</div><div class="about-val">Vanilla JS / HTML5 / WebOS IPK</div></div>' +
        '</div>' +

        '<div class="about-section">' +
          '<div class="about-section-title">致谢</div>' +
          '<div class="about-row"><div class="about-key">PiliPlus</div><div class="about-val">github.com/bggRGjQaUbCoE/PiliPlus</div></div>' +
        '</div>' +

        '<div class="about-note">这是 Hi！Bili 的 WebOS 参考版本。WebOS 设备已较少，本构建仅用于原型验证与接口演示。</div>' +

        '<button class="btn-danger" id="btnLogout">退出登录</button>' +
        '<button class="btn-ghost" id="btnBack">返回</button>' +
      '</div>';

    document.getElementById('btnLogout').addEventListener('click', function () {
      BiliApi.logout(function () {
        callbacks.onLogout();
      });
    });
    document.getElementById('btnBack').addEventListener('click', function () {
      callbacks.onBack();
    });
  }
  return { render: render };
})();
