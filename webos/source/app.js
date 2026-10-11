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

/* App — vanilla JS view router. */
(function () {
  var host = document.getElementById('viewHost');

  function clearHost() {
    host.innerHTML = '';
  }

  function showSplash() {
    SplashView.render(host, function () {
      route();
    });
  }

  function route() {
    if (AccountManager.isLoggedIn()) showMain();
    else showLogin();
  }

  function showLogin() {
    LoginView.destroy();
    LoginView.render(host, function () {
      showMain();
    });
  }

  function showMain() {
    MainView.render(host, {
      onAbout: showAbout,
      onLoggedOut: showLogin
    });
  }

  function showAbout() {
    AboutView.render(host, {
      onBack: showMain,
      onLogout: showLogin
    });
  }

  // Boot
  showSplash();
})();
