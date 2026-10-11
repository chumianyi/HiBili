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
 * BiliApi — Bilibili open API wrapper (vanilla JS, XMLHttpRequest).
 * Includes a self-contained RSA encryptor for password login.
 */
var BiliApi = (function () {
  var BASE = 'https://api.bilibili.com';
  var PASSPORT = 'https://passport.bilibili.com';
  var TICKET = 'https://passport.bilibili.com';

  /* ==================================================================
   *  Pure-JS RSA encryptor (PKCS#1 v1.5, public key encryption)
   *  Big integers represented as byte arrays (big-endian).
   * ================================================================== */
  var RSA = (function () {

    // ---- base64 ----
    function b64encode(bytes) {
      var s = '';
      for (var i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
      return btoa(s);
    }

    // ---- DER reader ----
    function derParse(buf) {
      var pos = 0;
      function readLen() {
        var b = buf[pos++];
        if (b & 0x80) {
          var n = b & 0x7F, len = 0;
          for (var i = 0; i < n; i++) len = (len << 8) | buf[pos++];
          return len;
        }
        return b;
      }
      function readTLV() {
        var tag = buf[pos++];
        var len = readLen();
        var val = buf.slice(pos, pos + len);
        pos += len;
        return { tag: tag, val: val };
      }
      function readSeq() {
        var seq = readTLV();
        if (seq.tag !== 0x30) throw new Error('DER: expected SEQUENCE, got 0x' + seq.tag.toString(16));
        // parse contents
        var items = [];
        var innerPos = 0;
        var inner = seq.val;
        while (innerPos < inner.length) {
          var savePos = pos;
          pos = innerPos;
          // TLV within inner
          var tag = inner[pos++];
          var b = inner[pos++];
          var len;
          if (b & 0x80) {
            var n = b & 0x7F; len = 0;
            for (var i = 0; i < n; i++) len = (len << 8) | inner[pos++];
          } else { len = b; }
          var v = inner.slice(pos, pos + len);
          pos += len;
          innerPos = pos;
          items.push({ tag: tag, val: v });
        }
        return items;
      }
      return { readSeq: readSeq };
    }

    // ---- big integer helpers (byte-array, big-endian) ----
    function ba(x) { return [x]; }
    function baCmp(a, b) {
      // strip leading zeros
      function strip(s) { var i = 0; while (i < s.length - 1 && s[i] === 0) i++; return s.slice(i); }
      a = strip(a); b = strip(b);
      if (a.length !== b.length) return a.length - b.length;
      for (var i = 0; i < a.length; i++) if (a[i] !== b[i]) return a[i] - b[i];
      return 0;
    }
    function baAdd(a, b) {
      var out = [], carry = 0;
      var i = a.length - 1, j = b.length - 1;
      while (i >= 0 || j >= 0 || carry) {
        var s = carry + (i >= 0 ? a[i--] : 0) + (j >= 0 ? b[j--] : 0);
        out.unshift(s & 0xFF); carry = s >> 8;
      }
      return out;
    }
    function baSub(a, b) {
      // assumes a >= b
      var out = [], borrow = 0;
      var i = a.length - 1, j = b.length - 1;
      while (i >= 0) {
        var d = a[i--] - borrow - (j >= 0 ? b[j--] : 0);
        if (d < 0) { d += 256; borrow = 1; } else borrow = 0;
        out.unshift(d);
      }
      // strip leading zeros
      var k = 0; while (k < out.length - 1 && out[k] === 0) k++;
      return out.slice(k);
    }
    function baMul(a, b) {
      // schoolbook, byte-at-a-time with 16-bit accumulator
      var res = new Array(a.length + b.length).fill(0);
      for (var i = a.length - 1; i >= 0; i--) {
        var carry = 0;
        for (var j = b.length - 1; j >= 0; j--) {
          var prod = res[i + j + 1] + a[i] * b[j] + carry;
          res[i + j + 1] = prod & 0xFF;
          carry = prod >> 8;
        }
        res[i] += carry;
      }
      var k = 0; while (k < res.length - 1 && res[k] === 0) k++;
      return res.slice(k);
    }
    function baDivMod(numer, denom) {
      // long division, returns remainder
      if (baCmp(numer, denom) < 0) return numer;
      // bit-by-bit long division
      var rem = [0];
      for (var i = 0; i < numer.length; i++) {
        // rem = rem << 8 | numer[i]
        rem.push(numer[i]);
        var k = 0; while (k < rem.length - 1 && rem[k] === 0) k++; rem = rem.slice(k);
        // while rem >= denom: rem -= denom
        while (baCmp(rem, denom) >= 0) rem = baSub(rem, denom);
      }
      return rem;
    }
    function baModExp(base, exp, mod) {
      // square-and-multiply, base^exp mod mod
      var result = [1];
      var b = baDivMod(base, mod);
      for (var i = 0; i < exp.length; i++) {
        var byte = exp[i];
        for (var bit = 7; bit >= 0; bit--) {
          result = baDivMod(baMul(result, result), mod);
          if ((byte >> bit) & 1) result = baDivMod(baMul(result, b), mod);
        }
      }
      return result;
    }

    // ---- parse PEM public key ----
    function parsePublicKey(pem) {
      // strip header/footer and newlines
      var b64 = pem.replace(/-----[A-Z ]+-----/g, '').replace(/\s+/g, '');
      var der = [];
      var bin = atob(b64);
      for (var i = 0; i < bin.length; i++) der.push(bin.charCodeAt(i));

      // SubjectPublicKeyInfo ::= SEQUENCE { algorithm SEQ, subjectPublicKey BIT STRING }
      var spki = derParse(der).readSeq();
      // spki[0] = algorithm (SEQUENCE), spki[1] = subjectPublicKey (BIT STRING, tag 0x03)
      var bitStr = spki[1].val;
      // first byte of BIT STRING content is number of unused bits (0)
      var rsaPubKeyDer = bitStr.slice(1);

      // RSAPublicKey ::= SEQUENCE { modulus INTEGER, publicExponent INTEGER }
      var rsaSeq = derParse(rsaPubKeyDer).readSeq();
      var modulus = rsaSeq[0].val;
      var exponent = rsaSeq[1].val;
      // strip leading zero byte from INTEGER if present
      if (modulus.length > 1 && modulus[0] === 0) modulus = modulus.slice(1);
      if (exponent.length > 1 && exponent[0] === 0) exponent = exponent.slice(1);
      return { n: modulus, e: exponent, keySize: modulus.length };
    }

    // ---- PKCS#1 v1.5 pad ----
    function pkcs1Pad(msg, emLen) {
      // EM = 0x00 || 0x02 || PS (non-zero random) || 0x00 || M
      // PS length = emLen - msg.length - 3, at least 8
      var psLen = emLen - msg.length - 3;
      if (psLen < 8) throw new Error('RSA: message too long');
      var ps = [];
      for (var i = 0; i < psLen; i++) {
        var b;
        do { b = Math.floor(Math.random() * 256); } while (b === 0);
        ps.push(b);
      }
      var em = [0x00, 0x02].concat(ps, [0x00], msg);
      return em;
    }

    /**
     * Encrypt a UTF-8 string with an RSA public key (PEM).
     * @param {string} message plaintext password
     * @param {string} pem PEM-encoded public key
     * @returns {string} base64-encoded ciphertext
     */
    function encrypt(message, pem) {
      var key = parsePublicKey(pem);
      // UTF-8 encode message
      var msgBytes = [];
      var utf8 = unescape(encodeURIComponent(message));
      for (var i = 0; i < utf8.length; i++) msgBytes.push(utf8.charCodeAt(i));

      var em = pkcs1Pad(msgBytes, key.keySize);
      // em is already a big-endian integer m
      var c = baModExp(em, key.e, key.n);
      // left-pad c to keySize bytes
      while (c.length < key.keySize) c.unshift(0);
      return b64encode(c);
    }

    return { encrypt: encrypt };
  })();

  /* ==================================================================
   *  Low-level XHR helper
   * ================================================================== */
  function request(method, url, params, headers, callback) {
    var xhr = new XMLHttpRequest();
    var query = '';
    if (method === 'GET' && params) {
      var pairs = [];
      var keys = Object.keys(params);
      for (var i = 0; i < keys.length; i++) {
        pairs.push(encodeURIComponent(keys[i]) + '=' + encodeURIComponent(params[keys[i]]));
      }
      query = '?' + pairs.join('&');
    }
    xhr.open(method, url + query, true);
    xhr.withCredentials = true;
    if (headers) {
      var hk = Object.keys(headers);
      for (var j = 0; j < hk.length; j++) xhr.setRequestHeader(hk[j], headers[hk[j]]);
    }
    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        try {
          var json = JSON.parse(xhr.responseText);
          if (json.code === 0) callback(null, json.data);
          else callback(new Error('BiliApi code=' + json.code + ' msg=' + json.message));
        } catch (e) {
          callback(e);
        }
      }
    };
    xhr.onerror = function () { callback(new Error('network error')); };
    if (method === 'POST') {
      var body = params;
      if (typeof params === 'object') {
        var fp = [];
        var fk = Object.keys(params);
        for (var k = 0; k < fk.length; k++) {
          fp.push(encodeURIComponent(fk[k]) + '=' + encodeURIComponent(params[fk[k]]));
        }
        body = fp.join('&');
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
      }
      xhr.send(body);
    } else {
      xhr.send();
    }
  }

  function cookieHeader() {
    var c = AccountManager.getCookies();
    var parts = [];
    if (c.SESSDATA) parts.push('SESSDATA=' + c.SESSDATA);
    if (c.bili_jct) parts.push('bili_jct=' + c.bili_jct);
    if (c.DedeUserID) parts.push('DedeUserID=' + c.DedeUserID);
    return parts.join('; ');
  }

  /* ==================================================================
   *  WBI key bootstrap
   * ================================================================== */
  function getNavInfo(callback) {
    request('GET', BASE + '/x/web-interface/nav', null, null, function (err, data) {
      if (!err && data && data.wbi_img) {
        SignUtil.setWbiKeys(data.wbi_img.img_url, data.wbi_img.sub_url);
      }
      callback(err, data);
    });
  }

  /* ==================================================================
   *  TV QR code login
   * ================================================================== */
  function qrcodeGenerate(callback) {
    var params = SignUtil.sign({
      appkey: SignUtil.APPKEY,
      ts: Math.floor(Date.now() / 1000)
    });
    request('GET', TICKET + '/x/passport-tv-login/qrcode/auth_code', params, null, callback);
  }

  function qrcodePoll(authCode, callback) {
    var params = SignUtil.sign({
      appkey: SignUtil.APPKEY,
      auth_code: authCode,
      local_id: 1,
      ts: Math.floor(Date.now() / 1000)
    });
    request('POST', TICKET + '/x/passport-tv-login/qrcode/poll', params, null, callback);
  }

  /* ==================================================================
   *  Password login (RSA-encrypted)
   * ================================================================== */

  /**
   * Fetch RSA public key for password login.
   * @param {function(?Error, {hash:string, key:string})} callback
   */
  function getWebKey(callback) {
    var params = SignUtil.sign({
      appkey: SignUtil.APPKEY,
      ts: Math.floor(Date.now() / 1000)
    });
    request('GET', PASSPORT + '/api/oauth2/getKey', params, null, callback);
  }

  /**
   * Log in with username + password.
   * Flow: getKey -> RSA encrypt password -> POST oauth2/login.
   * @param {string} username account name (phone/email/UID)
   * @param {string} password plaintext password
   * @param {function(?Error, object)} callback receives login data
   */
  function passwordLogin(username, password, callback) {
    getWebKey(function (err, keyData) {
      if (err) { callback(err); return; }
      var encryptedPwd;
      try {
        encryptedPwd = RSA.encrypt(password, keyData.key);
      } catch (e) {
        callback(new Error('RSA encryption failed: ' + e.message));
        return;
      }
      var params = SignUtil.sign({
        appkey: SignUtil.APPKEY,
        username: username,
        password: encryptedPwd,
        keep_phone: 'false',
        key: keyData.hash,
        ts: Math.floor(Date.now() / 1000)
      });
      request('POST', PASSPORT + '/api/oauth2/login', params, null, callback);
    });
  }

  /* ==================================================================
   *  Logout / user info
   * ================================================================== */
  function logout(callback) {
    request('POST', BASE + '/x/web-interface/nav/logout', {}, { 'Cookie': cookieHeader() }, function (err) {
      AccountManager.clear();
      callback(err);
    });
  }

  function getUserInfo(callback) {
    getNavInfo(callback);
  }

  /* ==================================================================
   *  Publish features (dynamic / reply / image upload)
   *  Full network implementations with WBI signing and CSRF tokens.
   * ================================================================== */

  /**
   * Publish a text/image dynamic.
   * @param {Object} opts
   * @param {string} opts.text    dynamic body text (max ~233 chars)
   * @param {string[]} [opts.images]  image URLs already uploaded via uploadImage()
   * @param {function(?Error, {dynamic_id:number, create_time:number})} callback
   *   Resolves with the new dynamic ID and server timestamp.
   */
  function createDynamic(opts, callback) {
    var params = SignUtil.wbiSign({
      appkey: SignUtil.APPKEY,
      text: opts.text || '',
      diy_add_image_list: (opts.images || []).join(','),
      csrf: AccountManager.getCookies().bili_jct,
      ts: Math.floor(Date.now() / 1000)
    });
    request('POST', BASE + '/x/v1/dynamic/publish/create', params, null, callback);
  }

  /**
   * Post a reply under a video / dynamic / comment.
   * @param {Object} opts
   * @param {number} opts.oid    object ID (video aid / dynamic ID)
   * @param {number} opts.type   business type (1=video, 11=dynamic, 17=activity)
   * @param {string} opts.message  reply text
   * @param {function(?Error, {rpid:number, mid:number, oid:number})} callback
   *   Resolves with the new reply ID and object reference.
   */
  function replyAdd(opts, callback) {
    var params = SignUtil.wbiSign({
      appkey: SignUtil.APPKEY,
      oid: opts.oid,
      type: opts.type,
      message: opts.message,
      csrf: AccountManager.getCookies().bili_jct,
      ts: Math.floor(Date.now() / 1000)
    });
    request('POST', BASE + '/x/v1/dynamic/reply/add', params, null, callback);
  }

  /**
   * Upload an image for use in a dynamic or reply.
   * @param {Blob} file  image blob (JPEG/PNG, <=10 MB)
   * @param {function(?Error, {image_url:string, image_width:number, image_height:number})} callback
   *   Resolves with the hosted image URL and dimensions.
   */
  function uploadImage(file, callback) {
    var xhr = new XMLHttpRequest();
    var url = BASE + '/x/v1/dynamic/reply/upload_image';
    var fd = new FormData();
    fd.append('file_up', file);
    fd.append('biz', 'dyn');
    fd.append('csrf', AccountManager.getCookies().bili_jct);
    xhr.open('POST', url, true);
    xhr.withCredentials = true;
    xhr.onreadystatechange = function () {
      if (xhr.readyState === 4) {
        try {
          var json = JSON.parse(xhr.responseText);
          if (json.code === 0) callback(null, json.data);
          else callback(new Error('uploadImage code=' + json.code));
        } catch (e) { callback(e); }
      }
    };
    xhr.onerror = function () { callback(new Error('network error')); };
    xhr.send(fd);
  }

  /* ==================================================================
   *  Public API
   * ================================================================== */
  return {
    getNavInfo: getNavInfo,
    getUserInfo: getUserInfo,
    qrcodeGenerate: qrcodeGenerate,
    qrcodePoll: qrcodePoll,
    getWebKey: getWebKey,
    passwordLogin: passwordLogin,
    logout: logout,
    createDynamic: createDynamic,
    replyAdd: replyAdd,
    uploadImage: uploadImage
  };
})();
