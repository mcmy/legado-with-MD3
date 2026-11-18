# canny_dev 本地功能保留清单

本清单是后续上游合并的核对基线。更新上游时须对照实现和测试逐项检查，不能用上游文件整体覆盖本地实现。

## 3.26.29 合并基线

- 合并前 HEAD：`3d9b92c28`。
- 上游标签：`3.26.29`，提交 `651330c9be85c54abecfcba0e79c52e74ad5deae`。
- 共同祖先：`35d013a0803931c333156d847ef1d1f3f324d83f`。
- 上游待合入 170 个提交；本地独有 14 个提交（含 4 个历史合并提交）。
- 合并前备份分支：`backup/canny_dev-before-3.26.29-20261118-122726`。
- 原暂存区和工作区分别保存为二进制 patch，另保留专用 stash；四个 AD Web 产物须在合并提交后恢复原状态，不属于此次合并内容。

## 必须保留的功能

| 功能 | 实现与核对入口 | 本次核对 |
| --- | --- | --- |
| Web 自定义阅读主题 | `ReadSettings.vue`、`bookStore.ts`；文字、页面、正文、弹窗颜色及重置 | 源码原样保留 |
| Web 快速切换主题 | `BookChapter.vue` 快捷入口与主题状态 | 源码原样保留 |
| Web 配置同步开关及冲突选择 | 默认本地保存；开启时选择浏览器覆盖备份或备份覆盖浏览器；`Backup.kt` 的 Web 配置备份 | 源码原样保留 |
| Web 阅读进度同步防回退 | `BookController.saveBookProgress` 的互斥、位置比较、条件 UPDATE、冲突返回；`ReturnData`、`BookProgress`、`BookDao`、`bookStore.ts` | 合并交叉处保留本地防回退和上游 DAO 新查询 |
| Web 阅读时长记录 | `/saveReadSession`、`ReadRecordRepository`、页面隐藏/退出刷新、重复时段去重、书架展示 | 路由与调用链保留；使用现有去重测试 |
| Web 滚轮一次滚动一页 | `BookChapter.vue` 的 wheel 节流与翻页行为 | 源码原样保留 |
| Web 五章滚动窗口与预取 | 当前章前后各两章、并发请求复用、失败重试、淘汰隔离、回收高度及锚点恢复、图片变化校正 | 现有 prefetch/window 测试通过 |
| Web 空格滚动 | 一屏保留一行与行距、Shift 反向、可配置时长、输入框/弹窗/修饰键保护 | 现有 space-scroll 测试通过 |
| Web 章节容错与语义位置恢复 | `ChapterContent.vue`、`loading.ts`、API 服务端偏移映射 | 现有恢复测试通过；源码原样保留 |
| Web 窄屏去边框 | `BookChapter.vue` 窄屏 `.chapter` 的 `border: none` | 源码原样保留 |
| Web 打包与内嵌入口 | `scripts/sync.js`、`assets/web/index.html`、`assets/web/vue` | 生产构建后产物与合并前逐字节一致 |
| Android 快捷阅读主题 | 菜单 AutoAwesome 按钮 → Intent → ViewModel → ReadStyleDelegate → Gateway/Repository；长按快捷按钮弹窗列表勾选、持久化与旧 JSON 默认值 | 全链路保留；使用现有 QuickReadStyle / JSON / ThemeListSelection 测试 |
| 阅读菜单动画状态修复 | 历史 `3ca4304de` 修改的是已在此前迁移中删除的 `ReadMenu.kt`；当前 owner 为 Compose `ReadBookMenuBar.kt` | 当前菜单实现保持合并前内容；不恢复旧 View，动画表现待真机复核 |
| 仓库本地约束与既有配置 | `AGENTS.md`、jsoup 1.16.2、Hutool 5.8.22、宽松 Base64 解码、applicationId、SDK、core/comic 产品维度 | 不放宽基线，不更改本地约束 |

## 合并处理

- 仅文档发生冲突：`feature-catalog.md` 接纳新增设置搜索与漫画设置 owner；`reader-semantic-contract.md` 在此前合并中被删除，此次恢复上游修订版，保留当前阅读器语义说明。
- 5 个 Kotlin 交叉文件及 4 份字符串资源检查：快捷主题字段、Intent 与分发、防回退 SQL、阅读时长接口和上游新增功能同时保留。
- 本地独有路径与合并前比较无内容变化；Web 重新构建没有产生差异。
- 上游新增 UI fork 按 gitlink 固定提交初始化，不修改 fork 或版本号。

## 本地独有提交

```text
3d9b92c28 feat: 优化浏览器空格滚动
92ac48b18 feat: 优化浏览器阅读体验
b38e7fa69 Merge tag '3.26.23' into canny_dev
922960cd8 fix: remove narrow reader side borders
5006f6fad feat: sync web reading records and restore quick theme switch
ce0902654 Merge tag '3.26.16-beta.40' into canny_dev
2e84e2b5c feat: 修改滚轮为一次性滚动一页
8c256e38a feat: 添加新界面切换主题按钮
071a86720 Merge remote-tracking branch 'origin/main' into canny_dev
ef331d579 fix: 修复web打包问题
e4d93dc54 Merge remote-tracking branch 'origin/main' into canny_dev
3ca4304de fix: 修复阅读菜单动画状态与 Web 章节加载容错问题
4db012265 feat: 添加浏览器配置同步开关
4a7aaa15d feat: 添加浏览器主题自定义，添加快速切换主题按钮（夜间模式不好用）
```

## 本地差异路径清单

以下按共同祖先比较，包含本地历史文档删除和生成产物；不代表每一项都是业务功能。

| 路径 | 合并核对方式 |
| --- | --- |
| `AGENTS.md` | 保留本地版本 |
| `app/src/main/assets/web/index.html` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookChapter-BCh_e27h.css` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookChapter-CPH2_fl2.css` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookChapter-CT0AqwN9.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookChapter-amfK5RXC.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookShelf-CLFT2RVM.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookShelf-DKZIf0K1.css` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/BookShelf-DiV04e_q.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/index-BP4cEHv-.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/index-C1k01oYZ.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/index-D6VBUV67.css` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/loading-BKNdgYqh.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/loading-DUiXIIPV.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/assets/vendor-BP94ZUjn.js` | 生产构建验证，产物一致 |
| `app/src/main/assets/web/vue/index.html` | 生产构建验证，产物一致 |
| `app/src/main/java/io/legado/app/api/ReturnData.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/api/controller/BookController.kt` | 交叉修改，逐项核对 |
| `app/src/main/java/io/legado/app/core/ui/morph/BookMorphState.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/data/dao/BookDao.kt` | 交叉修改，逐项核对 |
| `app/src/main/java/io/legado/app/data/entities/BookProgress.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/data/repository/ReadBookStyleConfigRepository.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/domain/gateway/ReadStyleGateway.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/domain/model/settings/QuickReadStyle.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/domain/model/settings/ReadStyleState.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/help/config/ReadBookConfig.kt` | 交叉修改，逐项核对 |
| `app/src/main/java/io/legado/app/help/storage/Backup.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/model/localBook/README.md` | 保留本地版本 |
| `app/src/main/java/io/legado/app/service/readaloud/ReadAloudOverlayWindow.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/ui/book/read/ReadBookContract.kt` | 交叉修改，逐项核对 |
| `app/src/main/java/io/legado/app/ui/book/read/ReadBookMenuBar.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/ui/book/read/ReadBookViewModel.kt` | 交叉修改，逐项核对 |
| `app/src/main/java/io/legado/app/ui/book/read/ReadStyleDelegate.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/ui/book/read/sheet/BgTextConfigSheet.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/ui/book/read/sheet/ReadAloudCastSheet.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/ui/widget/components/navigation/AppAdaptiveNavigation.kt` | 保留本地版本 |
| `app/src/main/java/io/legado/app/web/KtorServer.kt` | 保留本地版本 |
| `app/src/main/res/values-zh-rCN/strings.xml` | 交叉修改，逐项核对 |
| `app/src/main/res/values-zh-rHK/strings.xml` | 交叉修改，逐项核对 |
| `app/src/main/res/values-zh-rTW/strings.xml` | 交叉修改，逐项核对 |
| `app/src/main/res/values/strings.xml` | 交叉修改，逐项核对 |
| `app/src/test/java/io/legado/app/data/repository/ReadRecordRepositoryTest.kt` | 保留本地版本 |
| `app/src/test/java/io/legado/app/domain/model/settings/QuickReadStyleTest.kt` | 保留本地版本 |
| `app/src/test/java/io/legado/app/help/config/ReadConfigJsonRoundTripTest.kt` | 保留本地版本 |
| `app/src/test/java/io/legado/app/ui/book/read/ThemeListSelectionTest.kt` | 保留本地版本 |
| `app/src/test/resources/reader/semantic/README.md` | 保留本地版本 |
| `docs/dev/ai-context-management.md` | 保留本地版本 |
| `docs/dev/ai-image-providers.md` | 保留本地版本 |
| `docs/dev/bubble-package.md` | 保留本地版本 |
| `docs/dev/feature-catalog.md` | 交叉修改，逐项核对 |
| `docs/dev/http-tts-compatibility.md` | 保留本地版本 |
| `docs/dev/index.md` | 保留本地内容，追加本清单入口 |
| `docs/dev/manga-ai-calibration.md` | 保留本地版本 |
| `docs/dev/manga-ai-geometry-refinement.md` | 保留本地版本 |
| `docs/dev/manga-ai-response-recovery.md` | 保留本地版本 |
| `docs/dev/manga-ai-translation.md` | 保留本地版本 |
| `docs/dev/manga-ai-typesetting.md` | 保留本地版本 |
| `docs/dev/manga-cjk-calibration.md` | 保留本地版本 |
| `docs/dev/manga-cloud-ocr.md` | 保留本地版本 |
| `docs/dev/manga-complex-calibration.md` | 保留本地版本 |
| `docs/dev/manga-container-flow-layout.md` | 保留本地版本 |
| `docs/dev/manga-engine-extension-plan.md` | 保留本地版本 |
| `docs/dev/manga-engine-size-report.md` | 保留本地版本 |
| `docs/dev/manga-inpaint-engines.md` | 保留本地版本 |
| `docs/dev/manga-local-vision-models.md` | 保留本地版本 |
| `docs/dev/manga-settings.md` | 保留本地版本 |
| `docs/dev/manga-subsampling-plan.md` | 保留本地版本 |
| `docs/dev/manga-translation-engines.md` | 保留本地版本 |
| `docs/dev/manga-translation-settings.md` | 保留本地版本 |
| `docs/dev/manga-yakuyomi-review.md` | 保留本地版本 |
| `docs/dev/readaloud-player-morph.md` | 保留本地版本 |
| `docs/dev/reader-semantic-contract.md` | 恢复上游新文档 |
| `docs/http-tts-compatibility-audit-2026-10-17.md` | 保留本地版本 |
| `docs/superpowers/plans/2026-10-24-video-book-source.md` | 保留本地版本 |
| `docs/superpowers/specs/2026-10-24-video-book-source-design.md` | 保留本地版本 |
| `modules/web/package.json` | 保留本地版本 |
| `modules/web/scripts/chapter-prefetch.test.mjs` | 保留本地版本 |
| `modules/web/scripts/chapter-window.test.mjs` | 保留本地版本 |
| `modules/web/scripts/space-scroll.test.mjs` | 保留本地版本 |
| `modules/web/scripts/sync.js` | 保留本地版本 |
| `modules/web/src/api/api.ts` | 保留本地版本 |
| `modules/web/src/book.d.ts` | 保留本地版本 |
| `modules/web/src/components/ChapterContent.vue` | 保留本地版本 |
| `modules/web/src/components/PopCatalog.vue` | 保留本地版本 |
| `modules/web/src/components/ReadSettings.vue` | 保留本地版本 |
| `modules/web/src/hooks/loading.ts` | 保留本地版本 |
| `modules/web/src/main.ts` | 保留本地版本 |
| `modules/web/src/store/bookStore.ts` | 保留本地版本 |
| `modules/web/src/utils/chapterPrefetch.ts` | 保留本地版本 |
| `modules/web/src/utils/chapterWindow.ts` | 保留本地版本 |
| `modules/web/src/utils/spaceScrollDuration.ts` | 保留本地版本 |
| `modules/web/src/views/BookChapter.vue` | 保留本地版本 |
| `modules/web/src/views/BookShelf.vue` | 保留本地版本 |
| `modules/web/src/web.d.ts` | 保留本地版本 |

## 验证记录

- `pnpm --dir modules/web build`：13 个 Node 测试、TypeScript 检查、Vite 构建及 assets 同步通过。
- `git diff --check`、`git diff --cached --check`：通过。
- `verifyConfigArchitecture --no-configuration-cache`：通过。
- `scripts/prepare-ui-forks.py` 与 `--check`：通过（Python 3.11+；COUI 自带 daemon 使用固定 JDK 25，应用使用 JDK 21）。
- Android SDK 采用上游要求的 37.2；Miuix composite build 需要显式传入 SDK 环境变量，根 `local.properties` 不能单独满足其 SDK 查找。
- Core 全量单测：553 个测试类、3417 个用例，0 失败、0 错误、4 跳过；快捷主题、阅读记录、配置 JSON 和主题选择测试均通过。
- Comic 全量单测：561 个测试类、3450 个用例，0 失败、0 错误、1 跳过。
- 两个变体应用 Kotlin 编译通过。
- Core / Comic 的 lint、架构门禁、Debug APK 打包全部通过，最终构建 `BUILD SUCCESSFUL`。
- 两次中断均是本机资源调整：首次双变体并行 Kotlin 内存压力，随后 Gradle lint 内存压力。已通过串行编译及临时增加内存完成；未改项目内存配置。
- Core 的 4 个跳过用例是本地漫画模型控件；Comic 的 1 个跳过用例是 Core 专用行为。
- 单测实际执行命令如下；最终 lint 与打包使用同样环境和串行参数，另传入 `-Dorg.gradle.jvmargs=-Xmx8g -Xms512m -XX:MaxMetaspaceSize=2048m -XX:+UseG1GC -Dfile.encoding=UTF-8`，无需重跑已通过的单测。

```sh
JAVA_HOME=/Users/canny/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home \
ANDROID_HOME=/Users/canny/Library/Android/sdk \
ANDROID_SDK_ROOT=/Users/canny/Library/Android/sdk \
bash ./gradlew testAppCoreDebugUnitTest testAppComicDebugUnitTest \
  lintAppCoreDebug lintAppComicDebug verifyConfigArchitecture \
  assembleAppCoreDebug assembleAppComicDebug --continue --no-configuration-cache --max-workers=1 \
  '-Pkotlin.daemon.jvmargs=-Xmx6g -Xms256m -XX:MaxMetaspaceSize=1024m -XX:+UseG1GC'
```
- 未执行真机/浏览器连接设备服务的端到端验证，不能据此宣称全部运行时路径无回归。

## 快捷主题交互修订

- 删除背景/文字设置页中的单主题快捷开关；长按阅读菜单快捷主题按钮打开列表弹窗，每行使用 Switch 即时保存勾选。
- 快捷切换从当前阅读设置读取生效下标，避免 Gateway 缓存下标不更新导致只能切换一次。
- 按用户要求不执行全量测试，使用 Comic Release 编译、R8、资源和打包检查；交互由用户安装验证。

## Web 远程进度循环恢复修订

- Web 使用段落定位时保留 Android 段落内 UTF-16 偏移，防止定位后被观察器写回段首而触发重复冲突。
- 接受远程进度后页内重载章节，不再强制刷新浏览器；同一书籍已接受的位置保存在 sessionStorage，短页滚动钳制或刷新后不会重复提示同一远程位置，远程继续前进仍可提示。
- 异步冲突响应在展示和确认时检查当前书籍与最新位置，避免迟到的请求回退已前进的进度或修改另一本书。
- 仅执行针对性 Web 回归检查、类型检查、生产构建和 Comic Release 打包；不执行 Android 全量测试；后续按用户要求将本次修复提交到本地。

- 本次验证完成：Web 19 个快速用例、TypeScript 检查、生产构建通过；Comic Release 编译、R8、资源压缩和关键 lint、打包成功；三个 APK 内嵌 Web 资源与最新构建逐字节一致并通过 16 KB ZIP 对齐检查。APK 未签名，未做真机交互验证。
