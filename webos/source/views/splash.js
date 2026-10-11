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

/* Splash screen — blue gradient + icon + title, 2s. */
var SplashView = (function () {
  function render(container, onTimeout) {
    container.innerHTML =
      '<div class="hibili-splash">' +
        '<div class="splash-inner">' +
          '<img src="assets/icon-128.png" class="splash-icon" alt="Hi!Bili">' +
          '<div class="splash-title">Hi！Bili</div>' +
          '<div class="splash-sub">WebOS Reference Build v1.0.0</div>' +
        '</div>' +
      '</div>';
    setTimeout(function () {
      if (onTimeout) onTimeout();
    }, 2000);
  }
  return { render: render };
})();
