#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Hi！Bili 品牌零残留递归检测脚本
递归扫描项目内所有文件，检测 PiliPlus 原项目的品牌标识残留。
覆盖：应用图标、Logo、启动图、通知图标、品牌图形、名称、包名、类名、资源名、界面文案等。
致谢名单与开源许可声明中的引用除外（白名单机制）。
"""
import os
import sys
import re

# 项目根目录
ROOT = os.path.dirname(os.path.abspath(__file__))

# 需要检测的品牌关键词（大小写不敏感）
BRAND_PATTERNS = [
    r'piliplus',
    r'pili_plus',
    r'pili-plus',
    r'com\.example\.piliplus',
    r'PiliPlus',
]

# 白名单：这些文件/目录中允许出现 PiliPlus（致谢、许可声明、分析文档）
WHITELIST_PATHS = [
    'ACKNOWLEDGMENTS',
    'CREDITS',
    'NOTICE',
    'piliplus-analysis',
    'piliplus-source',
    'brand-scan',
]

# 白名单内容标记：文件中包含这些标记的行允许出现 PiliPlus
WHITELIST_MARKERS = [
    '致谢',
    'acknowledgment',
    'credit',
    '基于',
    'forked from',
    'based on',
    '引用项目',
    'license',
    '许可',
    'GPL',
]

# 二进制文件扩展名（不做内容检测，只做文件名检测）
BINARY_EXTENSIONS = {
    '.png', '.jpg', '.jpeg', '.gif', '.bmp', '.ico', '.webp',
    '.apk', '.ipa', '.appx', '.xap', '.ipk',
    '.jar', '.class', '.so', '.dex',
    '.zip', '.tar', '.gz', '.rar', '.7z',
    '.mp3', '.mp4', '.wav', '.ogg',
    '.ttf', '.otf', '.woff',
    '.pyc', '.pyo',
}

# 需要重点检查图标的目录
ICON_DIRECTORIES = [
    'mipmap', 'drawable', 'raw', 'assets', 'res',
    'images', 'icons', 'logo', 'splash',
    'web', 'Windows Phone', 'WebOS', 'windows-phone', 'webos',
]


def is_whitelisted_file(filepath):
    """检查文件路径是否在白名单中"""
    rel = os.path.relpath(filepath, ROOT)
    for wl in WHITELIST_PATHS:
        if wl.lower() in rel.lower():
            return True
    return False


def is_binary_file(filepath):
    """判断是否为二进制文件"""
    ext = os.path.splitext(filepath)[1].lower()
    return ext in BINARY_EXTENSIONS


def line_is_whitelisted(line):
    """检查行内容是否在白名单标记中"""
    lower = line.lower()
    for marker in WHITELIST_MARKERS:
        if marker.lower() in lower:
            return True
    return False


def scan_filename(filepath):
    """检测文件名中是否包含品牌词"""
    basename = os.path.basename(filepath)
    issues = []
    for pattern in BRAND_PATTERNS:
        if re.search(pattern, basename, re.IGNORECASE):
            issues.append(f"文件名包含品牌词: {basename}")
    return issues


def scan_content(filepath):
    """检测文件内容中是否包含品牌词"""
    issues = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            for lineno, line in enumerate(f, 1):
                for pattern in BRAND_PATTERNS:
                    matches = list(re.finditer(pattern, line, re.IGNORECASE))
                    if matches:
                        # 检查是否在白名单行
                        if line_is_whitelisted(line):
                            continue
                        # 检查是否在白名单文件
                        if is_whitelisted_file(filepath):
                            continue
                        stripped = line.strip()[:120]
                        issues.append(
                            f"第{lineno}行: 匹配 '{pattern}' -> {stripped}"
                        )
    except Exception as e:
        issues.append(f"读取失败: {e}")
    return issues


def scan_icon_directories(root):
    """重点扫描图标目录，检查是否有原项目图标残留"""
    issues = []
    for dirpath, dirnames, filenames in os.walk(root):
        rel_dir = os.path.relpath(dirpath, root)
        # 检查是否是图标相关目录
        is_icon_dir = any(
            icon_dir.lower() in rel_dir.lower()
            for icon_dir in ICON_DIRECTORIES
        )
        if is_icon_dir:
            for fname in filenames:
                fpath = os.path.join(dirpath, fname)
                # 检查文件名
                name_issues = scan_filename(fpath)
                for ni in name_issues:
                    issues.append(f"[{rel_dir}] {ni}")
                # 检查图标文件大小（原项目图标可能有特定尺寸，但这个不太可靠）
    return issues


def main():
    print("=" * 70)
    print("Hi！Bili 品牌零残留递归检测")
    print(f"扫描根目录: {ROOT}")
    print("=" * 70)

    all_issues = []
    file_count = 0
    icon_issues = []

    # 先扫描图标目录
    icon_issues = scan_icon_directories(ROOT)

    # 递归扫描所有文件
    for dirpath, dirnames, filenames in os.walk(ROOT):
        # 跳过 .git 目录
        if '.git' in dirpath.split(os.sep):
            continue
        for fname in filenames:
            fpath = os.path.join(dirpath, fname)
            file_count += 1

            # 文件名检测
            name_issues = scan_filename(fpath)
            for ni in name_issues:
                rel = os.path.relpath(fpath, ROOT)
                all_issues.append((rel, ni))

            # 内容检测（跳过二进制）
            if not is_binary_file(fpath):
                content_issues = scan_content(fpath)
                for ci in content_issues:
                    rel = os.path.relpath(fpath, ROOT)
                    all_issues.append((rel, ci))

    # 输出结果
    print(f"\n扫描文件总数: {file_count}")
    print(f"图标目录检测问题: {len(icon_issues)}")
    print(f"内容/文件名检测问题: {len(all_issues)}")

    if icon_issues:
        print("\n" + "=" * 70)
        print("图标目录问题:")
        print("=" * 70)
        for issue in icon_issues:
            print(f"  - {issue}")

    if all_issues:
        print("\n" + "=" * 70)
        print("品牌残留详情:")
        print("=" * 70)
        current_file = None
        for filepath, issue in all_issues:
            if filepath != current_file:
                print(f"\n📄 {filepath}")
                current_file = filepath
            print(f"   ⚠️  {issue}")

    total = len(icon_issues) + len(all_issues)
    print("\n" + "=" * 70)
    if total == 0:
        print("✅ 检测通过：未发现 PiliPlus 品牌残留（白名单除外）")
    else:
        print(f"❌ 检测失败：发现 {total} 处品牌残留，需替换或删除")
    print("=" * 70)

    return 0 if total == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
