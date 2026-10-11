# Hi！Bili

基于 PiliPlus 接口规范二次开发的多平台 B 站第三方客户端，统一蓝色视觉体系，GPL 3.0 开源。

## 版本概览

| 版本 | 平台 | 技术栈 | 主题 | 登录 | 发布 | 兼容目标 |
|------|------|--------|------|------|------|----------|
| 主版本 | Android 5.0+ | Java 原生 + Android SDK | Miuix 蓝色 | TV扫码+密码 | 动态+评论+图片 | minSdk 21 |
| 参考版本 | Android 1.0+ | 纯 Java 第一代 | 精美蓝色 | TV扫码+密码 | 动态+评论(简化) | minSdk 1 |
| 参考版本 | WebOS | Vanilla JS + HTML5 | 蓝色 | TV扫码+密码 | 接口已实现(UI预留) | webOS / Open webOS |
| 参考版本 | Windows Phone | C# + XAML (Silverlight 8.1) | 蓝色 Metro | TV扫码+密码 | 接口已实现(UI预留) | WP 8.1 / Win10 Mobile |

## 目录结构

```
HiBili/
├── android-5.0/       # 主版本：Android 5.0+ 原生 Java
├── android-1.0/       # 参考版本：Android 1.0+ 纯 Java
├── webos/             # 参考版本：WebOS (IPK)
├── windows-phone/     # 参考版本：Windows Phone (XAP/APPX)
├── docs/              # 项目文档
├── icons/             # 图标源文件
├── brand-scan.py      # 品牌零残留递归检测脚本
├── LICENSE            # GPL 3.0
└── README.md
```

## 构建

所有版本均通过 GitHub Actions 云端编译，无需本地构建环境。推送代码后自动触发。

各版本的构建配置位于对应目录的 `.github/workflows/` 中。

## 开源协议

GNU General Public License v3.0。基于 PiliPlus（GPL v3）二次开发，致谢名单见各版本关于页面。

## 项目主页

https://github.com/chumianyi/HiBili
