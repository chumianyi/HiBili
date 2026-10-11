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
 * SignUtil — MD5 + WBI signing for Bilibili API.
 * Vanilla JS, no dependencies.
 */
var SignUtil = (function () {
  var APPKEY = 'dfca71928277209b';
  var APPSEC = 'b5475a8825547a4fc26c7d518eaaa02e';

  var _imgKey = '';
  var _subKey = '';
  var _wbiCached = false;

  /* ---- MD5 (RFC 1321 reference, compact JS port) ---- */
  function toUtf8(s) { return unescape(encodeURIComponent(s)); }

  function add32(a, b) { return (a + b) & 0xFFFFFFFF; }

  function cmn(q, a, b, x, s, t) {
    a = add32(add32(a, q), add32(x, t));
    return add32((a << s) | (a >>> (32 - s)), b);
  }
  function ff(a, b, c, d, x, s, t) { return cmn((b & c) | ((~b) & d), a, b, x, s, t); }
  function gg(a, b, c, d, x, s, t) { return cmn((b & d) | (c & (~d)), a, b, x, s, t); }
  function hh(a, b, c, d, x, s, t) { return cmn(b ^ c ^ d, a, b, x, s, t); }
  function ii(a, b, c, d, x, s, t) { return cmn(c ^ (b | (~d)), a, b, x, s, t); }

  function md5cycle(x, k) {
    var a = x[0], b = x[1], c = x[2], d = x[3];
    a = ff(a, b, c, d, k[0], 7, -680876936);
    a = ff(a, b, c, d, k[1], 12, -389564586);
    a = ff(a, b, c, d, k[2], 17, 606105819);
    a = ff(a, b, c, d, k[3], 22, -1044525330);
    a = ff(a, b, c, d, k[4], 7, -176418897);
    a = ff(a, b, c, d, k[5], 12, 1200080426);
    a = ff(a, b, c, d, k[6], 17, -1473231341);
    a = ff(a, b, c, d, k[7], 22, -45705983);
    a = ff(a, b, c, d, k[8], 7, 1770035416);
    a = ff(a, b, c, d, k[9], 12, -1958414417);
    a = ff(a, b, c, d, k[10], 17, -42063);
    a = ff(a, b, c, d, k[11], 22, -1990404166);
    a = ff(a, b, c, d, k[12], 7, 1804603682);
    a = ff(a, b, c, d, k[13], 12, -40341101);
    a = ff(a, b, c, d, k[14], 17, -1502002290);
    a = ff(a, b, c, d, k[15], 22, 1236535329);
    a = gg(a, b, c, d, k[1], 5, -165796510);
    a = gg(a, b, c, d, k[6], 9, -1069501632);
    a = gg(a, b, c, d, k[11], 14, 643717713);
    a = gg(a, b, c, d, k[0], 20, -373897302);
    a = gg(a, b, c, d, k[5], 5, -701558691);
    a = gg(a, b, c, d, k[10], 9, 38016083);
    a = gg(a, b, c, d, k[15], 14, -660478335);
    a = gg(a, b, c, d, k[4], 20, -405537848);
    a = gg(a, b, c, d, k[9], 5, 568446438);
    a = gg(a, b, c, d, k[14], 9, -1019803690);
    a = gg(a, b, c, d, k[3], 14, -187363961);
    a = gg(a, b, c, d, k[8], 20, 1163531501);
    a = gg(a, b, c, d, k[13], 5, -1444681467);
    a = gg(a, b, c, d, k[2], 9, -51403784);
    a = gg(a, b, c, d, k[7], 14, 1735328473);
    a = gg(a, b, c, d, k[12], 20, -1926607734);
    a = hh(a, b, c, d, k[5], 4, -378558);
    a = hh(a, b, c, d, k[8], 11, -2022574463);
    a = hh(a, b, c, d, k[11], 16, 1839030562);
    a = hh(a, b, c, d, k[14], 23, -35309556);
    a = hh(a, b, c, d, k[1], 4, -1530992060);
    a = hh(a, b, c, d, k[4], 11, 1272893353);
    a = hh(a, b, c, d, k[7], 16, -155497632);
    a = hh(a, b, c, d, k[10], 23, -1094730640);
    a = hh(a, b, c, d, k[13], 4, 681279174);
    a = hh(a, b, c, d, k[0], 11, -358537222);
    a = hh(a, b, c, d, k[3], 16, -722521979);
    a = hh(a, b, c, d, k[6], 23, 76029189);
    a = hh(a, b, c, d, k[9], 4, -640364487);
    a = hh(a, b, c, d, k[12], 11, -421815835);
    a = hh(a, b, c, d, k[15], 16, 530742520);
    a = hh(a, b, c, d, k[2], 23, -995338651);
    a = ii(a, b, c, d, k[0], 6, -198630844);
    a = ii(a, b, c, d, k[7], 10, 1126891415);
    a = ii(a, b, c, d, k[14], 15, -1416354905);
    a = ii(a, b, c, d, k[5], 21, -57434055);
    a = ii(a, b, c, d, k[12], 6, 1700485571);
    a = ii(a, b, c, d, k[3], 10, -1894986606);
    a = ii(a, b, c, d, k[10], 15, -1051523);
    a = ii(a, b, c, d, k[1], 21, -2054922799);
    a = ii(a, b, c, d, k[8], 6, 1873313359);
    a = ii(a, b, c, d, k[15], 10, -30611744);
    a = ii(a, b, c, d, k[6], 15, -1560198380);
    a = ii(a, b, c, d, k[13], 21, 1309151649);
    a = ii(a, b, c, d, k[4], 6, -145523070);
    a = ii(a, b, c, d, k[11], 10, -1120210379);
    a = ii(a, b, c, d, k[2], 15, 718787259);
    a = ii(a, b, c, d, k[9], 21, -343485551);
    x[0] = add32(a, x[0]);
    x[1] = add32(b, x[1]);
    x[2] = add32(c, x[2]);
    x[3] = add32(d, x[3]);
  }

  function md5blk(s) {
    var out = [], i;
    for (i = 0; i < 64; i += 4) {
      out[i >> 2] = s.charCodeAt(i) + (s.charCodeAt(i + 1) << 8) +
        (s.charCodeAt(i + 2) << 16) + (s.charCodeAt(i + 3) << 24);
    }
    return out;
  }

  function rhex(n) {
    var s = '', j = 0;
    for (; j < 4; j++) {
      s += ((n >> (j * 8 + 4)) & 0x0F).toString(16) + ((n >> (j * 8)) & 0x0F).toString(16);
    }
    return s;
  }

  function md51(s) {
    var n = s.length;
    var state = [1732584193, -271733879, -1732584194, 271733878];
    var i;
    for (i = 64; i <= s.length; i += 64) {
      md5cycle(state, md5blk(s.substring(i - 64, i)));
    }
    s = s.substring(i - 64);
    var tail = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];
    for (i = 0; i < s.length; i++) tail[i >> 2] |= s.charCodeAt(i) << ((i % 4) << 3);
    tail[i >> 2] |= 0x80 << ((i % 4) << 3);
    if (i > 55) { md5cycle(state, tail); for (i = 0; i < 16; i++) tail[i] = 0; }
    tail[14] = n * 8;
    md5cycle(state, tail);
    return state;
  }

  function md5(input) {
    var state = md51(toUtf8(input));
    var hex = '';
    for (var i = 0; i < state.length; i++) hex += rhex(state[i]);
    return hex;
  }

  /* ---- AppKey sign ---- */
  function sign(params) {
    params = params || {};
    params.appkey = APPKEY;
    var keys = Object.keys(params).sort();
    var pairs = [];
    for (var i = 0; i < keys.length; i++) {
      pairs.push(encodeURIComponent(keys[i]) + '=' + encodeURIComponent(params[keys[i]]));
    }
    var query = pairs.join('&') + APPSEC;
    params.sign = md5(query);
    return params;
  }

  /* ---- WBI ---- */
  function keyOf(u) {
    if (!u) return '';
    var m = u.split('/').pop();
    if (!m) return '';
    return m.split('.')[0];
  }

  var MIXIN_KEY_ENC_TAB = [
    46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,
    27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,
    37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,
    22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52
  ];

  function mixinKey(orig) {
    var ret = '';
    for (var i = 0; i < 32; i++) ret += orig[MIXIN_KEY_ENC_TAB[i]];
    return ret;
  }

  function setWbiKeys(imgUrl, subUrl) {
    _imgKey = mixinKey(keyOf(imgUrl) + keyOf(subUrl));
    _wbiCached = true;
  }

  function wbiSign(params) {
    params = params || {};
    if (!_wbiCached) return params;
    params.wts = Math.floor(Date.now() / 1000);
    var cleaned = {};
    var keys = Object.keys(params).sort();
    for (var i = 0; i < keys.length; i++) {
      var k = keys[i];
      cleaned[k] = String(params[k]).replace(/[!'()*]/g, '');
    }
    keys = Object.keys(cleaned).sort();
    var pairs = [];
    for (var j = 0; j < keys.length; j++) {
      pairs.push(encodeURIComponent(keys[j]) + '=' + encodeURIComponent(cleaned[keys[j]]));
    }
    cleaned.w_rid = md5(pairs.join('&') + _imgKey);
    return cleaned;
  }

  return {
    APPKEY: APPKEY,
    md5: md5,
    sign: sign,
    setWbiKeys: setWbiKeys,
    wbiSign: wbiSign
  };
})();
