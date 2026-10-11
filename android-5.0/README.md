# Hi！Bili — Android 5.0+

原生 Java 实现的 B 站第三方客户端，Miuix（MIUI 设计语言）蓝色主题。

- 包名：`com.hibili.app`
- minSdk 21（Android 5.0），targetSdk / compileSdk 33
- 纯 Java，原生 Android View 体系（无 Kotlin / Compose / Flutter）
- 开源协议：**GPL v3.0**

## 功能

- TV 扫码登录（轮询 `auth_code`）+ 用户名密码登录（RSA 加密密码）
- AppKey 签名 & WBI 签名双机制
- 动态发布（纯文字，scene=1）
- 视频评论发布（type=1）
- 登录态持久化（SharedPreferences），自动注入 Cookie / access_key
- 关于页（版本号 / GitHub / GPL 声明 / 致谢名单 / 退出登录）

## 致谢

本项目在接口规范层面参考了 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus)，并使用如下开源依赖：

- [OkHttp](https://github.com/square/okhttp)
- [Gson](https://github.com/google/gson)
- [zxing](https://github.com/zxing/zxing)

## 编译

本地不要求编译；推送 `main` 或手动触发 GitHub Actions 即可生成 release APK：

```
.github/workflows/build-android-5.0.yml
```

产物以 `app-release.apk` 的形式上传为 workflow artifact。

## 仓库

https://github.com/chumianyi/HiBili
