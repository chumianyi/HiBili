# Hi！Bili — Windows Phone 参考版本

> ⚠️ **这是一个参考 / 原型版本。** Windows Phone 8.1 / Windows 10 Mobile 平台已于 2017–2020 年陆续停止支持，
> 本工程仅用于展示 Hi！Bili 在 Silverlight on Windows Phone 技术栈上的登录与页面骨架，
> 不提供完整的视频流、弹幕、评论等功能。请勿在真实设备上用于生产。

- **应用名**：Hi！Bili
- **命名空间**：`HiBili.WP`
- **技术栈**：C# + XAML，Windows Phone Silverlight 8.1
- **主题**：蓝色 Metro / Modern 风格（强调色 `#1E88E5`）
- **开源协议**：GPL v3.0

## 目录结构

```
windows-phone/
├── HiBili.WP.sln                 # Visual Studio 2015 解决方案
├── HiBili.WP/
│   ├── HiBili.WP.csproj          # Silverlight for Windows Phone 8.1 工程
│   ├── App.xaml(.cs)              # 应用入口
│   ├── MainPage.xaml(.cs)         # 根占位页（实际启动页为 Views/SplashPage）
│   ├── Views/
│   │   ├── SplashPage             # 蓝色启动页，2 秒后路由
│   │   ├── LoginPage              # Pivot：扫码登录 / 密码登录
│   │   ├── MainPage               # Panorama：首页 / 动态 / 我的（含示例数据卡片）
│   │   └── AboutPage              # 版本、GitHub、GPL、致谢、退出登录
│   ├── Services/
│   │   ├── BiliApiService         # HTTP API 封装 + 发布接口扩展点
│   │   ├── SignService            # AppKey MD5 签名 + WBI w_rid
│   │   └── AccountService         # IsolatedStorage 凭证存储
│   ├── Models/                    # UserInfo / LoginResult / ApiResponse
│   ├── Helpers/                   # MD5 / RSA / QR 渲染
│   ├── Assets/                    # 真实 Hi！Bili 图标 / 磁贴 / 启动图
│   ├── Properties/                # AssemblyInfo + WMAppManifest
│   └── Resources/                # AppResources.resx + ThemeStyles.xaml
├── LICENSE                        # GPL v3.0
└── .github/workflows/build-windows-phone.yml
```

## 已实现功能

1. **TV 扫码登录**：调用 `passport.bilibili.tv/x/passport-tv-login/qrcode/auth_code` 取二维码，
   通过 goqr.me 渲染为图片，`DispatcherTimer` 每 2 秒轮询 `poll`，成功后落库 token。
2. **密码登录表单**：走 `/x/passport-login/web/key` → RSA 加密 → `/x/passport-login/oauth2/login`。
3. **凭证存储**：`IsolatedStorageSettings` 保存 access_token / refresh_token / cookie / 昵称。
4. **主页 Panorama**：首页 / 动态 / 我的三个枢纽，首页与动态渲染示例卡片数据，显示登录昵称。
5. **关于页**：版本号、GitHub、GPL 声明、致谢名单、退出登录。

## 预留的发布接口扩展点

`BiliApiService` 中以下方法已实现完整的 HTTP 请求逻辑（WBI 签名 + `bili_jct` CSRF +
form/multipart 封装），仅**未在 XAML 页面上挂发布入口**（均带 `// TODO: 发布接口扩展点，UI 暂未接入`）：

- `CreateDynamicAsync(text, atUids, atTopics)` — POST `/x/vc/dynamic/create` 发文字动态
- `ReplyAddAsync(oid, type, message, root, parent)` — POST `/x/v2/reply/add` 发评论
- `UploadImageAsync(pngBytes, biz)` — multipart POST `/x/vc/dynamic/upload-img` 上传图片

后续接入发布功能时，在 UI 上调用这些方法即可，无需补网络层。

## 编译

需要 **Windows + Visual Studio 2015（或 2017/2019 安装 Windows Phone 8.1 SDK 组件）**。

```powershell
msbuild HiBili.WP.sln /p:Configuration=Release /p:Platform=ARM
```

GitHub Actions 已配置 `windows-latest` 构建（见 `.github/workflows/build-windows-phone.yml`）。
由于托管镜像上未必预装了 Windows Phone 8.1 遗留 SDK，CI 构建可能需要在 runner 上额外安装
`Microsoft.VisualStudio.Component.WindowsPhone81SDK`；本仓库不保证在 CI 上 100% 通过，
但保证源码与工程文件结构完整、可在本地 VS 中打开。

## 致谢

- 感谢 [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) 在 B 站第三方客户端协议研究上的参考贡献。
  本工程在致谢名单之外**不保留任何 PiliPlus 品牌标识**。

## 协议

[GPL v3.0](./LICENSE) © 2024 HiBili Team
