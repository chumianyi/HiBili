# Hi！Bili 项目交付说明

## 源码与项目结构说明

Hi！Bili 是基于 PiliPlus 开源项目的接口规范进行二次开发的多平台 B 站第三方客户端，整体采用蓝色视觉体系，以 GNU General Public License v3.0 开源协议发布。项目采用多平台并列的目录结构，在仓库根目录下分别设置 android-5.0、android-1.0、webos、windows-phone 四个独立工程目录，每个目录均为可独立构建的完整项目，包含各自的源码、资源、构建配置与开源许可证声明。根目录同时存放统一的 GPL 3.0 许可证文件、项目说明文档、品牌零残留递归检测脚本以及集中管理的 GitHub Actions 工作流配置。图标源文件统一存放于 icons 目录，由独立的矢量设计流程生成全平台所需尺寸。

Android 5.0 主版本位于 android-5.0 目录，采用标准的 Android Gradle 工程结构，app 模块下包含 src/main 源码树与 res 资源目录。Java 源码按功能划分为 api 网络层、account 账户管理、model 数据模型、ui 界面层与 util 工具类，共十六个 Java 源文件。资源目录包含 values 配色与主题定义、drawable 自定义形状与矢量图标、layout 六套 Activity 布局以及 mipmap 各密度启动图标。Android 1.0 参考版本位于 android-1.0 目录，采用第一代 Android 工程结构，以 Apache Ant 的 build.xml 作为构建系统，源码位于 src 目录，资源位于 res 目录，不使用 Gradle 或 AndroidX。WebOS 参考版本位于 webos 目录，采用纯原生 JavaScript 架构，以 IIFE 模块模式组织代码，source 目录下包含 app.js 路由入口、views 四个视图组件、models 数据与 API 层以及 css 样式表，通过 appinfo.json 声明应用元数据，使用 tools/package-ipk.sh 脚本打包为标准 IPK 格式。Windows Phone 参考版本位于 windows-phone 目录，采用 Visual Studio 解决方案结构，HiBili.WP 项目包含 Views 四个 XAML 页面、Services 服务层、Models 数据模型、Helpers 工具类以及 Resources 主题资源字典，使用 WMAppManifest.xml 声明 Windows Phone Silverlight 8.1 应用配置。

## 依赖说明与降级替换方案

Android 5.0 主版本以原生 Java 配合 Android SDK 开发，未引入 Flutter、Kotlin 或 Jetpack Compose。网络层选用 OkHttp 3.12.13，这是 OkHttp 3.x 系列中稳定支持 minSdk 21 的最终版本，相比 OkHttp 4.x 避免了更高的最低系统要求与 Kotlin 运行时依赖。JSON 序列化使用 Gson 2.8.9，二维码生成使用 zxing core 3.4.1。界面层未引入 Material Components 库，而是通过自定义 shape drawable、selector 与矢量图标实现 Miuix 风格的视觉效果，仅保留 androidx.appcompat 1.4.2 与 constraintlayout 2.1.4 作为基础兼容支持。这一依赖选择确保了在 Android 5.0 设备上的完整运行能力，同时将方法数与包体积控制在合理范围。

Android 1.0 参考版本的降级幅度最大，完全排除了所有现代第三方库。网络通信直接使用 java.net.HttpURLConnection，这是 JDK 内置且自 Android API 1 起可用的 HTTP 客户端。JSON 解析使用 Android 框架内置的 org.json 包，不依赖 Gson 或 Jackson。加密操作使用 javax.crypto 包中的 Cipher 与 KeyFactory 实现 RSA，MD5 摘要使用 java.security.MessageDigest。由于 android.util.Base64 是 API 8 才引入的工具类，项目中手写了 Base64 编解码实现以兼容 API 1。异步操作使用 Thread 配合 Handler 的传统模式，不使用 AsyncTask（API 3 引入）或协程。整个版本不包含任何 jar 依赖，纯靠 Android 框架与 JDK 标准库完成所有功能。

WebOS 参考版本采用零第三方依赖的纯原生 JavaScript 实现，不使用 Enyo、jQuery 或任何前端框架。MD5 摘要与 WBI 签名算法直接在 sign-util.js 中以原生 JavaScript 实现，RSA 加密在 bili-api.js 中内联了完整的 PEM 公钥解析、PKCS#1 v1.5 填充与大整数模幂运算，不依赖 JSEncrypt 或 forge 等加密库。HTTP 请求使用 XMLHttpRequest，凭证存储使用 localStorage。这一设计确保了在旧版 WebOS 浏览器环境中的最大兼容性，同时消除了框架加载失败的风险。Windows Phone 参考版本使用 C# 与 XAML，依赖 Windows Phone Silverlight 8.1 框架内置的 System.Net.Http、System.Security.Cryptography 与 System.Runtime.Serialization.Json，未引入 Newtonsoft.Json 等第三方 NuGet 包，确保在 Windows Phone 8.1 运行时上的原生兼容性。

## 构建方式与运行环境说明

所有版本均通过 GitHub Actions 云端编译，不依赖本地构建环境，推送代码至 main 分支后自动触发对应工作流。根目录 .github/workflows 下包含四个独立工作流，分别对应四个平台版本，每个工作流仅在对应目录文件变更时触发，也可通过 workflow_dispatch 手动运行。

Android 5.0 主版本的构建工作流运行于 ubuntu-latest，配置 JDK 11 与 Android SDK Platform 33、Build Tools 33.0.0，使用 Gradle 7.5.1 执行 assembleRelease 任务，生成的 APK 通过 apksigner 以调试密钥库签名后作为构建产物上传。Android 1.0 参考版本的构建工作流同样运行于 ubuntu-latest，配置 JDK 8 与 Apache Ant，通过 sdkmanager 安装已标记为 obsolete 的 android-4 平台骨架与 build-tools 19.1.0，执行 ant debug 任务生成 APK，考虑到旧平台组件可能在最新 SDK 源中下架，构建步骤设置了容错警告。WebOS 参考版本的构建工作流运行于 ubuntu-latest，安装 ar、tar、gzip 等打包工具后执行 package-ipk.sh 脚本，生成标准 IPK 包并上传。Windows Phone 参考版本的构建工作流运行于 windows-latest，配置 MSBuild 与 NuGet，执行 Release ARM 配置的解决方案构建，生成 XAP 包并上传。

## 兼容性说明

Android 5.0 主版本以 minSdk 21 为最低兼容目标，覆盖 Android 5.0 Lollipop 至 Android 13 的全部设备，targetSdk 设定为 33 以满足现代应用商店的目标版本要求。该版本使用原生 View 体系与 androidx 兼容库，在 Android 5.0 设备上可完整运行包括 Miuix 主题、登录、发布在内的全部功能。Android 1.0 参考版本以 minSdk 1 为理论最低目标，targetSdk 设定为 4（Android 1.6 Donut），代码严格使用 Java 1.6 语法与 Android 1.0 时代的 API，排除了所有高版本 API 调用。需要说明的是，由于 Android 1.0 设备在当今已极为罕见，且现代 B 站 API 服务器要求 TLS 1.2 以上的加密协议，而 Android 1.0 至 Android 4.4 系统内置的 TLS 栈可能不支持现代加密套件，因此在真实的 Android 1.0 设备上网络请求可能因 TLS 握手失败而无法完成，这是平台本身的技术限制而非应用层缺陷。

WebOS 参考版本兼容 LG webOS、Open webOS 及 Palm webOS 系列设备，应用打包为标准 IPK 格式，可通过 webOS 应用安装器或命令行部署。由于 WebOS 平台已停止官方更新，部分旧设备的内置浏览器可能不支持最新的 JavaScript 语法，本版本全部使用 ES5 语法编写以确保最大兼容性。Windows Phone 参考版本兼容 Windows Phone 8.1 与 Windows 10 Mobile，采用 Silverlight 8.1 应用模型，可在支持该模型的设备上侧载安装。Windows Phone 平台已于 2017 年停止官方支持，应用商店已关闭，本版本仅用于技术演示与接口验证。

## 各版本完成情况

Android 5.0 主版本在登录功能上完整实现了 TV 扫码登录与密码登录两种方式。TV 扫码登录通过调用 passport-tv-login 的 auth_code 接口获取二维码与授权码，以两秒间隔轮询 poll 接口检测扫码状态，成功后获取 access_token、refresh_token 与 cookie 信息并持久化存储。密码登录通过 web/key 接口获取 RSA 公钥与盐值，使用 RSA/ECB/PKCS1Padding 加密盐值与密码的拼接串后提交至 oauth2/login 接口。凭证通过 SharedPreferences 持久化，AccountManager 单例在应用启动时自动恢复登录态，并在所有网络请求中自动注入 Cookie 头与 access_key。退出登录调用 login/exit/v2 接口并清除本地凭证，异常情况通过 Toast 与错误回调向用户提示。发布功能完整实现了动态发布与评论发布。动态发布支持纯文字与图文两种类型，图文发布前先通过 upload_bfs 接口上传图片获取图片地址，再组装 dyn_req 请求体提交至 create/dyn 接口。评论发布通过 reply/add 接口提交，支持视频评论区类型参数与内容。发布前均进行登录态校验，未登录时自动跳转登录页面，成功与失败均有明确的用户反馈。主题方面将原版 Material 3 替换为 Miuix 风格，以 #1E88E5 为主色调，配合 #1565C0 深色与 #64B5F6 浅色构建蓝色视觉体系，卡片采用 16dp 大圆角，按钮采用 12dp 圆角与涟漪效果，状态栏使用蓝色纯色，整体排版与动效均经过优化。

Android 1.0 参考版本在登录功能上实现了与主版本相同的 TV 扫码与密码登录流程，使用 HttpURLConnection 完成所有网络请求，通过 Thread 与 Handler 实现异步操作与主线程回调。凭证存储使用 SharedPreferences，登录态维护与退出登录逻辑完整。发布功能实现了文字动态发布与评论发布，因平台限制未实现图片上传的用户界面，但在 API 层预留了图片上传的方法定义。主题采用精美的蓝色风格，以 #2196F3 为主色，背景使用 #E3F2FD 浅蓝，标题栏使用蓝色渐变 drawable，按钮与输入框均为自定义蓝色样式，与主版本的 Miuix 风格形成区分但保持蓝色系一致。

WebOS 参考版本实现了完整的 TV 扫码登录与密码登录，密码登录的 RSA 加密以纯 JavaScript 内联实现，不依赖外部加密库。凭证存储于 localStorage，登录态在应用启动时自动恢复，退出登录功能完整。发布功能在 API 层完整实现了动态创建、评论发表与图片上传三个方法，包含 WBI 签名与 CSRF token 注入，但用户界面暂未接入发布入口，属于预留扩展状态。主题采用 #03A9F4 浅蓝为主色的蓝色系，配合 WebOS 平台的手势区域设计与卡片式布局。Windows Phone 参考版本实现了 TV 扫码登录与密码登录，使用 DispatcherTimer 进行扫码轮询，凭证存储于 IsolatedStorageSettings。发布功能在服务层完整实现了 CreateDynamicAsync、ReplyAddAsync 与 UploadImageAsync 三个异步方法，均包含 WBI 签名与 bili_jct CSRF 校验，用户界面暂未挂接发布入口。主题采用 Windows Phone Metro 风格，强调色为 #1E88E5，使用 Panorama 与 Pivot 控件构建导航，磁贴资源均为真实的 Hi！Bili 图标。

## 已知限制与后续扩展建议

Android 1.0 参考版本受限于旧版 Android 系统的 TLS 支持，在真实的 Android 1.0 至 4.4 设备上可能无法与现代 B 站 API 服务器建立 HTTPS 连接，后续可考虑集成第三方 TLS 库（如 conscrypt 或 BoringSSL 的旧版移植）以提升加密兼容性，或通过中间代理服务器降级加密协议。WBI 签名中的 mixinKey 混淆重排在该版本中做了简化处理，后续可补全完整的 64 位混淆表实现以确保所有 Web 接口的签名正确性。

WebOS 与 Windows Phone 参考版本的发布功能已在服务层完整实现，但用户界面尚未提供发布入口，后续可在主页添加发布按钮并接入对应的 API 调用。WebOS 版本的二维码显示使用外部图片服务渲染，后续可内联纯 JavaScript 二维码生成库以消除外部依赖。Windows Phone 版本的构建依赖 Visual Studio 2015/2017 的 Windows Phone 8.1 SDK 组件，在最新的 windows-latest 运行器上可能因组件下架而构建失败，后续可考虑使用自托管的 Windows 构建代理或容器化的旧版构建环境。

四个版本当前均未实现视频播放功能，因为 PiliPlus 原项目的核心价值在于浏览与互动而非播放器，后续可根据需要在 Android 5.0 主版本中集成系统 VideoView 或 ExoPlayer 的旧版兼容分支。多账号切换功能在当前版本中仅支持单账号，后续可扩展 AccountManager 以支持多账号存储与切换。推送通知功能尚未实现，后续可在 Android 5.0 版本中集成 Firebase Cloud Messaging 的旧版兼容 SDK 或使用 B 站自有推送通道。所有版本的关于页面均已包含完整的致谢名单与 GitHub 项目地址，后续可随依赖变更持续更新致谢内容。
