# Hi！Bili — WebOS Reference Build

> **Reference / prototype build.** WebOS devices (LG smart TVs, Open webOS,
> Palm Pre) are now a very small install base. This build exists to prove
> that the Hi！Bili architecture — Bilibili API signing, TV qrcode login,
> credential storage, RSA password login — runs on a vanilla JS WebOS app.
> It is **not** intended as a production TV client.

- **App ID:** `com.hibili.webos`
- **Version:** 1.0.0
- **Stack:** Vanilla JavaScript / HTML5 / CSS (no framework)
- **Theme:** Light Blue (`#03A9F4`)
- **License:** GPL v3.0
- **Project:** <https://github.com/chumianyi/HiBili>

## What works

| Area | Status |
|---|---|
| Splash screen (2 s) | ✅ |
| TV qrcode login (generate → poll → save credentials) | ✅ |
| Password login (RSA-encrypted via built-in pure-JS RSA) | ✅ |
| WBI + AppKey MD5 signing | ✅ |
| User info feed with dynamic / video / favorites / history cards | ✅ |
| About / acknowledgements / logout | ✅ |
| Publish dynamic / reply / upload image (network implementations) | ✅ |

## Layout

```
webos/
├── appinfo.json          # WebOS app manifest
├── index.html            # Entry point
├── source/
│   ├── app.js            # View router
│   ├── views/           # splash / login / main / about
│   ├── models/           # sign-util / account-manager / bili-api
│   └── css/              # Blue theme (main + hibili-theme)
├── assets/               # icons + splash
├── tools/package-ipk.sh  # IPK builder (ar + tar.gz)
├── LICENSE               # GPL v3.0
└── .github/workflows/build-webos.yml
```

No third-party framework is required. The app is plain HTML + CSS + JS,
which is the native WebOS application model.

## Building

```bash
bash tools/package-ipk.sh hibili-webos_1.0.0_all.ipk
```

The CI workflow (`.github/workflows/build-webos.yml`) runs this on every push.

## Installing on webOS

Use `ares-install` from the webOS TV SDK:

```bash
ares-install hibili-webos_1.0.0_all.ipk
```

(Reference only — you need a developer-mode webOS TV / emulator.)

## Acknowledgements

- **PiliPlus** — <https://github.com/bggRGjQaUbCoE/PiliPlus> — reference
  implementation for Bilibili signing and TV qrcode login flows.
- **Enyo** — <https://github.com/enyojs/enyo> — Apache-2.0 licensed
  application framework originally built for HP/Palm/webOS.

No PiliPlus branding appears anywhere in this build (only this acknowledgement).
