# 阅读·LYX

这是基于 [LegadoTeam/legado](https://github.com/LegadoTeam/legado) 的个人定制版本，面向大书架、分组阅读、搜索筛选和数据迁移场景。

## APK 下载

- [直接下载阅读·LYX 3.26081613](https://github.com/xiaoxin0819/legado/releases/latest/download/legado_app_3.26081613_readingLYX.apk)
- [直接下载阅读·B 3.26081613](https://github.com/xiaoxin0819/legado/releases/latest/download/legado_app_3.26081613_readingB.apk)
- [直接下载 Debug 3.26081613](https://github.com/xiaoxin0819/legado/releases/latest/download/legado_app_3.26081613_debug.apk)
- [查看全部版本](https://github.com/xiaoxin0819/legado/releases)

APK 放在 GitHub Release 中，不会出现在仓库代码文件列表里。

## 主要功能

- 在当前书架分组内随机选择一本书阅读。
- 书架、书架管理、搜索结果和发现分类支持字数、题材与排序筛选。
- 发现分类每次连续加载 5 页，并支持筛选与批量加入书架。
- 书架管理可筛选后批量加入或移除分组。
- 合并导入备份时保留当前数据，并合并书籍、书源与分组关系。
- 提供存储清理、快速瘦身和恢复初始状态功能。
- 内置定制默认书源、订阅源、字体和“文字”阅读预设。
- 优化 4000-5000 本大书架的筛选、后台数据库监听、阅读发热和耗电。
- 取消启动自动更新；可在当前分组手动更新书架，并沿用已有线程数设置控制并发。

## 应用版本

| 版本 | 包名 | 签名用途 |
| --- | --- | --- |
| 阅读·LYX | `com.legado.app.lyx` | 独立 LYX 签名，可与其他版本同时安装 |
| 阅读·B | `com.legado.app.releaseB` | 保留旧 ReadingB 覆盖升级链 |
| Debug | `com.legado.app.debug` | 保留旧 Debug 覆盖升级链 |

阅读·LYX 的签名密钥仅保存在本地，不上传仓库。丢失密钥后将无法为已安装的阅读·LYX提供覆盖升级。

## 本地构建

Windows 下执行：

```powershell
.\gradlew.bat :app:assembleAppReleaseLYX '-Pksp.incremental=false'
```

签名配置文件：

```text
C:\Users\JZQ\.gradle\reading-lyx-signing.properties
```

构建产物：

```text
app\build\outputs\apk\app\releaseLYX\
```

本次发布成品：

```text
D:\codexproject\yuedu\legado_app_3.26081613_readingLYX.apk
D:\codexproject\yuedu\legado_app_3.26081613_readingB.apk
D:\codexproject\yuedu\legado_app_3.26081613_debug.apk
```

## 项目来源

- 上游项目：[LegadoTeam/legado](https://github.com/LegadoTeam/legado)
- 定制仓库：[xiaoxin0819/legado](https://github.com/xiaoxin0819/legado)

本项目继承上游 Legado 的开源协议和第三方依赖许可。
