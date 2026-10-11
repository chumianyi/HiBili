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
 * but WITHOUT ANY even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/* LoginView — QR code tab + password tab (RSA-encrypted). */
var LoginView = (function () {
  var qrTimer = null;
  var authCode = '';

  function render(container, onLoggedIn) {
    container.innerHTML =
      '<div class="hibili-login">' +
        '<div class="login-header">登录 Hi！Bili</div>' +
        '<div class="login-tabs">' +
          '<div class="login-tab active" data-tab="qr">扫码登录</div>' +
          '<div class="login-tab" data-tab="pwd">密码登录</div>' +
        '</div>' +

        '<div class="login-panel" id="qrPanel">' +
          '<div class="qr-wrap">' +
            '<img id="qrImg" class="qr-img" src="" alt="">' +
            '<div id="qrLoading" class="qr-loading">正在获取二维码…</div>' +
          '</div>' +
          '<div class="qr-hint">请用哔哩哔哩手机端「我的 → 右上角扫一扫」扫描二维码</div>' +
          '<div id="qrStatus" class="qr-status"></div>' +
          '<button class="btn-primary" id="btnRefreshQr">刷新二维码</button>' +
        '</div>' +

        '<div class="login-panel" id="pwdPanel" style="display:none">' +
          '<div class="form-row">' +
            '<div class="form-label">账号</div>' +
            '<input id="inpUser" class="form-input" placeholder="手机号 / 邮箱 / UID">' +
          '</div>' +
          '<div class="form-row">' +
            '<div class="form-label">密码</div>' +
            '<input id="inpPwd" type="password" class="form-input" placeholder="登录密码">' +
          '</div>' +
          '<div id="pwdError" class="form-error"></div>' +
          '<button class="btn-primary" id="btnPwdLogin">登录</button>' +
        '</div>' +
      '</div>';

    // Tab switching
    var tabs = container.querySelectorAll('.login-tab');
    tabs.forEach(function (tab) {
      tab.addEventListener('click', function () {
        tabs.forEach(function (t) { t.classList.remove('active'); });
        tab.classList.add('active');
        var which = tab.getAttribute('data-tab');
        document.getElementById('qrPanel').style.display = which === 'qr' ? '' : 'none';
        document.getElementById('pwdPanel').style.display = which === 'pwd' ? '' : 'none';
        if (which === 'pwd') stopPolling();
        else startQrLogin();
      });
    });

    document.getElementById('btnRefreshQr').addEventListener('click', function () {
      stopPolling();
      document.getElementById('qrImg').src = '';
      document.getElementById('qrLoading').textContent = '正在获取二维码…';
      startQrLogin();
    });

    document.getElementById('btnPwdLogin').addEventListener('click', function () {
      doPwdLogin(onLoggedIn);
    });

    startQrLogin();
  }

  function startQrLogin() {
    BiliApi.qrcodeGenerate(function (err, data) {
      if (err) {
        document.getElementById('qrLoading').textContent = '二维码获取失败：' + err.message;
        return;
      }
      authCode = data.auth_code;
      document.getElementById('qrImg').src = data.url;
      document.getElementById('qrLoading').textContent = '';
      document.getElementById('qrStatus').textContent = '等待扫描…';
      startPolling(onLoggedIn);
    });
  }

  function startPolling(onLoggedIn) {
    stopPolling();
    qrTimer = setInterval(function () {
      BiliApi.qrcodePoll(authCode, function (err, data) {
        if (err) {
          document.getElementById('qrStatus').textContent = '轮询出错：' + err.message;
          return;
        }
        if (data.code === 86037) {
          document.getElementById('qrStatus').textContent = '等待扫描…';
        } else if (data.code === 86038) {
          document.getElementById('qrStatus').textContent = '已扫描，请在手机上确认…';
        } else if (data.code === 0) {
          stopPolling();
          AccountManager.save({
            access_key: data.access_token,
            refresh_token: data.refresh_token,
            SESSDATA: data.cookie_info ? data.cookie_info.SESSDATA : '',
            bili_jct: data.cookie_info ? data.cookie_info.bili_jct : '',
            DedeUserID: data.cookie_info ? data.cookie_info.DedeUserID : ''
          });
          document.getElementById('qrStatus').textContent = '登录成功，正在进入…';
          if (onLoggedIn) onLoggedIn();
        } else {
          document.getElementById('qrStatus').textContent = '状态：' + data.message;
        }
      });
    }, 2000);
  }

  function stopPolling() {
    if (qrTimer) { clearInterval(qrTimer); qrTimer = null; }
  }

  function doPwdLogin(onLoggedIn) {
    var u = document.getElementById('inpUser').value.trim();
    var p = document.getElementById('inpPwd').value;
    var errEl = document.getElementById('pwdError');
    if (!u || !p) {
      errEl.textContent = '请输入账号和密码';
      return;
    }
    errEl.textContent = '';
    BiliApi.passwordLogin(u, p, function (err, data) {
      if (err) {
        errEl.textContent = err.message;
        return;
      }
      AccountManager.save({
        access_key: data.access_token,
        refresh_token: data.refresh_token,
        SESSDATA: data.cookie_info ? data.cookie_info.SESSDATA : '',
        bili_jct: data.cookie_info ? data.cookie_info.bili_jct : '',
        DedeUserID: data.cookie_info ? data.cookie_info.DedeUserID : ''
      });
      if (onLoggedIn) onLoggedIn();
    });
  }

  function destroy() {
    stopPolling();
  }

  return { render: render, destroy: destroy };
})();
