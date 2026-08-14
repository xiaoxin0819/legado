# LYX 定制版迁移与维护说明

## 本次基线

- 上游仓库：`LegadoTeam/legado`
- 上游分支：`master`
- 上游提交：`c0754342f7440063974e62eee96a3c5aae05e06e`
- 迁移日期：2026-07-21
- 旧定制工作区：`D:\codexproject\yuedu\LegadoTeam-legado`
- 新版独立目录：`D:\codexproject\yuedu\LegadoTeam-legado-20260721`
- 当前发布版本号：`versionCode 40006`

旧工作区仅作为功能参考，本次所有适配都写入新版独立目录。

## 已迁移功能

1. 当前书架分组内随机阅读。
2. 书架按字数、题材关键词筛选，并按字数、书名、作者排序。
3. 搜索结果筛选、自动补充后续结果和批量加入书架。
4. 发现分类页漏斗筛选，每次连续加载 5 页。
5. 书架管理筛选、筛选后批量分组，以及分组菜单崩溃修复。
6. 分组选择优先显示内存列表，减少大书架首次弹出卡顿。
7. 合并导入备份，保留现有数据并合并书籍、书源和分组关系。
8. 存储清理、数据库快速瘦身和恢复刚安装状态。
9. 默认书源、默认订阅源、默认字体和“文字”阅读预设。
10. 取消首次启动的隐私、密码等重复弹窗。
11. 移除书架菜单中的更新目录和远程书籍，合并为书单管理。
12. 关于页加入 LYX，并指向 `xiaoxin0819/legado`。
13. 保留 `releaseB` 构建类型；当前代码版本号继续递增到 40006。
14. 发现分类结果页恢复漏斗筛选、“第 X 页”和完整封面显示，并移除书架阅读进度横线。
15. 大书架阅读性能优化：搜索、发现和书架管理退到后台后停止书表监听，书架状态改用轻量索引，普通翻页进度最多每 2 分钟轻量落盘，并合并空闲备份计时任务。
16. 新增 `releaseLYX` 独立构建，应用名“阅读·LYX”，包名 `com.legado.app.lyx`，使用全新独立签名。

## 后续同步上游

不需要每次从头迁移。应长期保留两个远程和一条定制分支：

- `upstream` 指向 `https://github.com/LegadoTeam/legado.git`
- `origin` 指向 `https://github.com/xiaoxin0819/legado.git`
- `custom` 分支只保存 LYX 定制功能

建议把每组功能保持为独立提交，例如“随机阅读”“筛选”“存储清理”“默认数据”。上游发布新版时：

```bash
git fetch upstream
git switch custom
git merge upstream/master
```

Git 会自动合并没有重叠的改动。只有上游和定制版同时修改同一文件的同一段代码时才会产生冲突，此时逐个处理冲突、编译和回归测试即可。发布 APK 前继续递增 `versionCode`，同一包名和同一签名才能覆盖升级。

这次必须做完整迁移，是因为旧定制仓库被压成了没有父提交的单独历史，Git 无法自动建立它与上游之间的差异关系。新版完成后不应再次压掉与上游的共同历史。

## 每次更新检查

1. 记录同步前后的上游提交号。
2. 合并上游并处理冲突。
3. 检查随机阅读是否严格限定当前分组。
4. 检查书架、搜索、发现和书架管理四处筛选。
5. 检查发现页五页加载及筛选后的自动补充。
6. 使用大书架测试分组选择和筛选性能。
7. 测试合并恢复、存储清理和恢复默认状态。
8. 编译 Debug、ReleaseB 与 ReleaseLYX，核对包名、签名和 versionCode。
9. 更新本文件、应用更新日志和帮助文档。

## 2026-07-21 构建验证

- `:app:compileAppDebugKotlin`：通过。
- `:app:assembleAppDebug`：通过；包名 `com.legado.app.debug`，版本号 `40005`，版本名 `3.26072122debug`。
- `:app:assembleAppReleaseB`：通过；包名 `com.legado.app.releaseB`，版本号 `40005`，版本名 `3.26072122`。
- Debug 证书 SHA-256：`5734eaca62c3bd5c0b5564112660a15919e13317409855818592e61a45ef803e`，与旧 Debug 相同。
- ReleaseB 证书 SHA-256：`93a28468b0f69e8d14c8a99ab45841cef902bbba3761bbfee02e67cba801563e`，与旧 ReadingB 相同。
- 两个旧版版本号均为 `40004`，新版均为 `40005`，包名与各自签名保持不变，因此满足直接覆盖升级条件。
- 工作区成品：`D:\codexproject\yuedu\legado_app_3.26072122_debug.apk` 与 `D:\codexproject\yuedu\legado_app_3.26072122_readingB.apk`。

最新上游的 `htmlunit-core-js` 包含 Android 26 才原生支持的 `MethodHandle/VarHandle` 字节码。普通 Debug 的 D8 在 `minSdk 23` 下会失败；本次仅在打包时临时对 Debug 启用 R8，APK 生成后已经恢复 `minifyEnabled false`。后续打 Debug 包仍需采用同样的临时步骤，或等待上游依赖修复，不应为了打包永久提高应用的最低 Android 版本。

## 2026-08-14 阅读·LYX

- 构建任务：`:app:assembleAppReleaseLYX`。
- 包名：`com.legado.app.lyx`。
- 显示名称：`阅读·LYX`。
- 签名配置保存在本机 `C:\Users\JZQ\.gradle\reading-lyx-signing.properties`，密钥位于 `C:\Users\JZQ\.android\reading-lyx-release.jks`，均不得提交到 GitHub。
- 签名证书 SHA-256：`a8fd704c0b43dc22ac86b598f64221470e599c7e085255ac1f51e8dc24c3f4a5`。
- `:app:assembleAppReleaseLYX`：通过；版本号 `40006`，版本名 `3.26081422`。
- APK SHA-256：`9ab52646ef665180716f162af13c11047cf3cc95bc46b49e86d3f663f073b1f8`。
- 工作区成品：`D:\codexproject\yuedu\legado_app_3.26081422_readingLYX.apk`。
