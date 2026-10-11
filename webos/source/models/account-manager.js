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
 * AccountManager — credential storage via localStorage.
 */
var AccountManager = (function () {
  var KEY = 'hibili.webos.account';

  function save(data) {
    try {
      localStorage.setItem(KEY, JSON.stringify(data));
    } catch (e) { /* storage unavailable; session-only */ }
  }

  function load() {
    try {
      var raw = localStorage.getItem(KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) {}
    return null;
  }

  function clear() {
    try { localStorage.removeItem(KEY); } catch (e) {}
  }

  function isLoggedIn() {
    var a = load();
    return !!(a && (a.SESSDATA || a.access_key));
  }

  function getCookies() {
    var a = load();
    if (!a) return { SESSDATA: '', bili_jct: '', DedeUserID: '' };
    return {
      SESSDATA: a.SESSDATA || '',
      bili_jct: a.bili_jct || '',
      DedeUserID: a.DedeUserID || ''
    };
  }

  function getAccessKey() {
    var a = load();
    return (a && a.access_key) || '';
  }

  return {
    save: save,
    load: load,
    clear: clear,
    isLoggedIn: isLoggedIn,
    getCookies: getCookies,
    getAccessKey: getAccessKey
  };
})();
