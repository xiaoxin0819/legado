# 阅读 B 定制版

这是基于 [LegadoTeam/legado](https://github.com/LegadoTeam/legado) 修改的个人定制版本，保留原项目的开源阅读能力，并围绕大书架、分组随机阅读、搜索筛选、默认书源、默认阅读样式和存储清理做了定制优化。

## APK 下载

当前发布版 APK 已上传到 GitHub Release：

- [下载阅读·B 版](https://github.com/xiaoxin0819/legado/releases/latest/download/legado_app_3.26.070713_readingB.apk)
- [下载 Debug 版](https://github.com/xiaoxin0819/legado/releases/latest/download/legado_app_3.26.070713_debug.apk)
- [查看 Release 页面](https://github.com/xiaoxin0819/legado/releases/latest)

GitHub 的代码文件列表不会自动显示 APK，大文件安装包会放在 Release 里下载。

## 主要功能

### 书架随机阅读

- 在书架菜单中新增随机阅读入口。
- 随机范围跟随当前书架分组。
- 例如在 A 分组中触发随机阅读，只会从 A 分组的书籍里随机；在 B 分组中触发，只会从 B 分组随机。

### 书架筛选与排序

- 新增书目筛选能力。
- 支持按字数范围筛选。
- 支持按题材关键词包含或排除筛选。
- 支持按默认顺序、字数、书名、作者排序。
- 书架管理页新增筛选入口，可先筛出目标书籍，再全选或框选后批量加入/移出分组。
- 针对 3000 到 4000 本以上的大书架做了筛选性能优化，避免一次性重计算导致明显卡顿。

### 搜索与发现页筛选

- 搜索结果页新增筛选和排序。
- 搜索结果支持批量添加当前可见书籍。
- 发现源分类页右上角新增筛选按钮，可使用同一套字数、题材、排序规则。
- 发现源分页加载调整为每次加载 5 页，兼顾结果数量和流畅度。

### 存储清理与恢复默认

- 新增存储清理/瘦身入口。
- 支持扫描并清理真实文件缓存，包括离线缓存、无效书籍缓存、应用缓存、WebView 缓存、日志、临时备份、规则数据、本地封面等。
- 移除无效或体验不明确的深度数据库压缩入口，避免大数据量下长时间卡死。
- 新增恢复默认状态能力，用于恢复到接近刚安装后的状态，本地书文件除外。
- 新增合并导入备份入口，保留当前数据，只导入备份里的书籍、书源和书架分组。
- 合并导入时会重映射分组关系，同名分组复用，已有书籍只追加分组关系，不覆盖当前阅读进度。

### 默认书源与订阅源

- 内置默认书源数据，不依赖短期有效的云盘链接。
- 默认订阅源移除“小说拾遗”。
- 新增源仓库订阅源：
  - `https://www.yckceo.com/yuedu/rss/json/id/193.json`
  - `https://www.yckceo.com/yuedu/rss/json/id/723.json`

### 默认阅读样式

- 内置默认字体文件。
- 新增并默认使用“文字”阅读背景预设。
- 默认阅读参数按定制要求调整，包括字号、字距、行距、段距、边距、翻页动画、隐藏页眉页脚线等。

### 界面与说明

- 初始安装时取消不必要的三个弹窗。
- 书架右上角菜单移除“更新目录”和“远程书籍”，保留下拉刷新。
- “导入书单”和“导出书单”合并为“书单管理”。
- 关于页开发人员新增 LYX，链接保持原样。
- 更新日志和帮助文档已补充 2026.7.7 的新增功能说明。

## 安装说明

当前 APK 位于项目外层目录：

```text
D:\codexproject\yuedu\legado_app_3.26.070713_readingB.apk
D:\codexproject\yuedu\legado_app_3.26.070713_debug.apk
```

如果设备上已经安装同一签名的调试版，可以直接覆盖安装。

```powershell
adb install -r D:\codexproject\yuedu\legado_app_3.26.070713_debug.apk
adb install -r D:\codexproject\yuedu\legado_app_3.26.070713_readingB.apk
```

## 构建方式

Windows 下可在源码目录执行：

```powershell
.\gradlew.bat :app:assembleAppDebug --console=plain --stacktrace --no-daemon --max-workers=2 "-Dorg.gradle.vfs.watch=false"
```

构建产物通常位于：

```text
app\build\outputs\apk\app\debug\
```

## 项目来源

本项目基于 Legado 开源项目修改：

- 上游项目：[LegadoTeam/legado](https://github.com/LegadoTeam/legado)
- 当前定制仓库：[xiaoxin0819/legado](https://github.com/xiaoxin0819/legado)

## 开源协议

本项目继承上游 Legado 的开源协议。请遵守原项目许可证和相关第三方依赖的开源协议。
