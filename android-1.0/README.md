# Hi！Bili — Android 1.0+ 参考版本（第一代纯 Java）

> 本目录是 **Hi！Bili** 的 Android 1.0+ 参考实现，用于展示“第一代 Android”形态下的
> 第三方 B 站客户端骨架。它与主版本（现代 Android）共享接口设计，但完全基于
> **纯 Java、API 1 时代 API** 编写，仅作参考与教学用途。

- 应用名：**Hi！Bili**
- 包名：`com.hibili.classic`
- minSdk = **1**（Android 1.0），targetSdk = **4**（Android 1.6）
- 语言：纯 Java（Java 1.6 语法），**不使用** Kotlin / lambda / Stream / AndroidX / Material / 协程
- 主题：精美蓝色系（Blue 500/700/100 + 浅蓝背景）
- 协议：**GPL 3.0**

## 与现代版的差异（技术约束）

| 能力 | 现代版 | 本参考版 |
|------|--------|----------|
| 网络 | OkHttp | `java.net.HttpURLConnection` / 内置 `org.apache.http` |
| JSON | Gson | `org.json.JSONObject/JSONArray` 手写解析 |
| 异步 | RxJava / Coroutine / AsyncTask | `Thread` + `android.os.Handler` |
| 凭证 | 高级存储 | `android.content.SharedPreferences` |
| 加密 | 第三方库 | `java.security.MessageDigest`(MD5) + `javax.crypto`(RSA)，手写 Base64 |
| 二维码 | zxing 生成 | WebView 加载二维码图片 URL |
| 构建 | Gradle / AGP | Apache Ant（`build.xml`） |

## 已实现页面

1. `SplashActivity` —— 蓝色渐变启动页
2. `LoginActivity` —— TV 扫码登录（WebView 二维码 + 轮询）/ 密码登录（RSA 加密）
3. `MainActivity` —— 登录状态 + 功能入口
4. `PublishActivity` —— 发布纯文字动态
5. `ReplyActivity` —— 发表评论
6. `AboutActivity` —— 关于 / GPL 声明 / 致谢 / 退出登录

## 接口（简化实现）

- TV 扫码：`POST /x/passport-tv-login/qrcode/auth_code` → `/poll`
- 密码：`GET /x/passport-login/web/key` → RSA 加密 → `POST /x/passport-login/oauth2/login`
- appkey 签名：`dfca71928277209b` + appsec MD5 签名
- WBI 签名：参考版本退化为 appkey 签名（保留方法，便于后续扩展）
- 发布：`POST /x/dynamic/feed/create/dyn`（scene=1）
- 评论：`POST /x/v2/reply/add`
- 登出：`POST /login/exit/v2`

## 目录结构

```
android-1.0/
├── AndroidManifest.xml
├── build.xml                 # Ant 构建
├── project.properties
├── default.properties
├── res/
│   ├── values/               # strings / colors / themes
│   ├── drawable/             # 蓝色 shape / selector / gradient
│   ├── layout/               # 6 个页面布局
│   └── drawable-mdpi/        # 图标位图（可由 hibili-icons 复制）
├── src/com/hibili/classic/
│   ├── HiBiliApp.java
│   ├── api/                  # BiliApi、SignUtil
│   ├── account/              # AccountManager
│   ├── ui/                   # 6 个 Activity
│   └── util/                 # Md5Util、RsaUtil、HttpUtil、Base64Util
├── LICENSE                   # GPL 3.0
├── README.md
└── .github/workflows/build-android-1.0.yml
```

## 已知限制（重要）

- **设备极少**：minSdk=1 的真实设备在现代几乎不存在，本版本主要用于展示第一代写法。
- **TLS 兼容性**：现代 B 站接口均为 HTTPS，而 Android 1.0 时代的 TLS 栈无法协商
  现代 TLS（1.2/1.3），在真机上请求大概率因握手失败而不可用。本仓库重点是
  接口流程、签名逻辑与 UI 骨架，不保证联网可用。
- **WBI 签名简化**：未实现 img_key/sub_key 混淆重排，相关接口可能返回 -799 等错误。
- **图片上传**：仅预留扩展点，参考版本未实现图片动态发布 UI。
- **图标**：`ic_launcher` 用 shape drawable 占位；正式发布请从
  `hibili-icons/android-1.0/` 复制 mdpi PNG 到 `res/drawable-mdpi/`。

## 构建

本地需要 Android SDK（platform `android-4` + build-tools）。CI 见
`.github/workflows/build-android-1.0.yml`，产物为 debug APK artifact。

## 致谢

感谢 **PiliPlus** 及 B 站社区的开源贡献，本参考版本借鉴了其接口设计思路。
本项目与哔哩哔哩官方无任何隶属关系。

## 协议

本程序依据 **GPL 3.0** 协议发布，详见 [LICENSE](./LICENSE)。

项目地址：https://github.com/chumianyi/HiBili
