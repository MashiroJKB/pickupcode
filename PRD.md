# 「取件码」（`PickupCode`）Android 原生应用完整产品与技术规范文档（PRD）

> **目标版本**：`2.3.3`（`versionCode = 20`）  
> **文档定位**：本文档为面向 AI 编码引擎与原生 Android 工程师的**1:1 复现级完整工程规格说明书**，完整沉淀了产品定位、视觉规范、交互细节、动画参数、解析引擎、桌面生态及全部已知避坑约束，阅读本文档即可从零完整重构出一模一样的应用程序。

---

## 1. 产品定位与核心原则

**「取件码」** 是一款极简、高颜值的 Android 本地取件码管理工具。纯本地离线运行，每次打开自动扫描最近 7 天短信，智能拆分并过滤干扰短信，按快递房顺序与货架号自然排序整理，同时提供淘宝/菜鸟/拼多多身份码快捷直达、批量勾选核销、图片分享清单、桌面角标与桌面液态玻璃小组件。

### 1.1 绝对隐私与离线约束
1. **严格零联网权限**：`AndroidManifest.xml` 中**绝不声明** `INTERNET`、`ACCESS_NETWORK_STATE` 或任何网络通信权限；同样不声明任何相册/外部存储读写权限（仅使用 `FileProvider` 在分享图片时授权单次临时只读 URI）。
2. **仅使用短信与必要系统权限**：
   - `android.permission.READ_SMS`
   - `android.permission.RECEIVE_SMS`
   - `android.permission.POST_NOTIFICATIONS`（Android 13+ 桌面角标通知载体）
   - 国产厂商桌面角标权限（华为、荣耀、三星、OPPO/一加、vivo）：
     - `com.huawei.android.launcher.permission.CHANGE_BADGE`
     - `com.hihonor.android.launcher.permission.CHANGE_BADGE`
     - `com.sec.android.provider.badge.permission.READ` / `.WRITE`
     - `com.oppo.launcher.permission.READ_SETTINGS` / `.WRITE_SETTINGS`
     - `com.oplus.notification.permission.BADGE_DIGITAL`
     - `com.vivo.notification.permission.BADGE_ICON`
   - 声明 `<uses-feature android:name="android.hardware.telephony" android:required="false" />`。
   - 声明 `<queries>` 包可见性：`com.taobao.taobao`、`com.xunmeng.pinduoduo`。
3. **禁止展示或识别快递品牌/代收点别名**：不识别、不存储、不显示任何快递品牌（如“中通快递”、“京东快递”）或代收点名称；数据库预留字段 `expressCompany` 始终置空 `""`。

### 1.2 明确排除项（禁止擅自添加）
- **不要**包含「截图解析 / OCR / 照片选择器（Photo Picker） / ML Kit」等任何图片识别功能（维持极致轻量化与零多余三方重型依赖，Release 安装包体积约 `2.56 MiB`）。
- **不要**在取件码卡片、弹窗或分享图片中计算并展示「已到 N 天 / 滞留天数」标签。
- **不要**保留「以文本分享」或「仅复制文本」的分享入口；分享弹窗仅保留**「以图片分享」**。
- **不要**使用 `NotificationListenerService`（通知监听服务）；仅靠 `READ_SMS` + `SMS_RECEIVED` 广播 + `ContentObserver(content://sms)` 加上手动录入/粘贴短信。
- **不要**在首页顶部「设置」按钮展示「设置」两字，右上角设置入口在正常模式下只保留简洁的圆形齿轮图标；设置页顶部「设置」二字仅作静态展示，不可点击返回，返回仅保留左上角圆圈内的 `<` 按钮。
- **不要**在应用内任何底部、弹窗或页签保留不优雅的孤儿文字折行（所有短标签、胶囊按钮与次要单行文本均需防折行保护）。

---

## 2. 技术栈、构建配置与工程结构

### 2.1 SDK 与核心依赖版本
- **包名（`applicationId` / `namespace`）**：`com.example.pickupcode`
- **版本配置**：`versionCode = 20`，`versionName = "2.3.3"`
- **SDK 配置**：`minSdk = 29`（Android 10）、`targetSdk = 35`、`compileSdk = 37`、`buildToolsVersion = "36.0.0"`、`JavaVersion.VERSION_17`
- **Gradle 与插件版本**（`gradle/libs.versions.toml`）：
  - AGP：`9.5.1`（`com.android.application`）
  - Kotlin：`2.3.20`（`kotlin-compose`）
  - KSP：`2.3.6`
  - Compose BOM：`2024.11.00`（`ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`）
  - AndroidX Core KTX：`1.15.0`
  - Activity Compose：`1.9.3`
  - Lifecycle Runtime KTX / Runtime Compose / ViewModel Compose：`2.8.7`
  - Room Runtime / Room KTX / Room Compiler (KSP)：`2.7.2`
  - DataStore Preferences：`1.1.1`
  - Kotlinx Coroutines Android：`1.9.0`
  - JUnit：`4.13.2`
- **Release 构建与优化配置**（`app/build.gradle.kts`）：
  - 开启 `isMinifyEnabled = true` 与 `isShrinkResources = true`，使用 `proguard-android-optimize.txt` + `proguard-rules.pro`（保留 `com.example.pickupcode.data.**` 实体类防 R8 混淆）。
  - Release 使用 `signingConfigs.getByName("debug")` 以保持覆盖升级签名一致。
  - `MainActivity` 开启 `enableEdgeToEdge()`（状态栏/导航栏全透明并随系统深/浅色模式自动切换图标颜色）以及 `enableHighRefreshRate()`（优先选择同分辨率下最高 `refreshRate` 的 `Display.Mode`，如 120Hz）。

### 2.2 完整工程目录结构

```text
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── drawable/
│   │   ├── ic_launcher_foreground.xml  # 108dp 等轴测线性快递盒前景矢量图标（跟随夜间模式切黑白）
│   │   ├── ic_stat_pickup.xml          # 通知小图标
│   │   ├── widget_btn_done.xml         # 小组件右侧「已取」胶囊背景
│   │   ├── widget_glass_bg.xml         # 小组件外框 24dp 液态玻璃渐变背景
│   │   └── widget_row_bg.xml           # 小组件内行 14dp 卡片背景
│   ├── drawable-nodpi/
│   │   ├── ic_brand_cainiao.png        # 菜鸟应用图标（原生大图）
│   │   ├── ic_brand_pinduoduo.png      # 拼多多应用图标（原生大图）
│   │   └── ic_brand_taobao.webp        # 淘宝应用图标（原生大图）
│   ├── layout/
│   │   ├── widget_pickup_code.xml      # 桌面小组件整体布局
│   │   └── widget_pickup_item.xml      # 桌面小组件单条待取件列表行布局
│   ├── mipmap-anydpi-v26/
│   │   ├── ic_launcher.xml             # 自适应矢量桌面图标（含 monochrome 主题图标适配）
│   │   └── ic_launcher_round.xml       # 圆形自适应矢量桌面图标
│   ├── values/colors.xml & themes.xml & strings.xml
│   ├── values-night/colors.xml & themes.xml
│   └── xml/
│       ├── file_paths.xml              # cache-path: shared_images/ 供图片分享
│       └── pickup_widget_info.xml      # 4×3（250×160dp）水平垂直可缩放桌面小组件元数据
└── java/com/example/pickupcode/
    ├── PickupApplication.kt            # 初始化 Room/Settings/Repository，注册角标通知通道与 SmsContentObserver
    ├── MainActivity.kt                 # 120Hz 刷新率设置、系统深浅色自适应、权限流水线、全局 BackHandler、主页与设置页切换、弹窗调度
    ├── data/
    │   ├── local/
    │   │   ├── PickupItemEntity.kt     # Room 表 `pickup_items`、圆圈数字符号映射、NaturalCodeComparator 自然排序
    │   │   ├── PickupDao.kt            # 查询、批量更新、清理过期已取记录
    │   │   ├── PickupDatabase.kt       # `pickup_code_local.db` 单例
    │   │   └── SettingsDataStore.kt    # DataStore 偏好设置、`StationRule`、默认 1~8 号规则、口令导入导出
    │   ├── parser/
    │   │   └── PickupMessageParser.kt  # 前置拦截、干扰数字掩码、双策略多码拆分、站房匹配、商品备注提取
    │   └── repository/
    │       └── PickupRepository.kt     # 互斥锁防重入、跨来源合并、7天短信全量扫库与清理、`reapplyStationRules`
    ├── service/
    │   ├── SmsReceiver.kt              # 短信广播 `SMS_RECEIVED`（支持长短信按发件人拼接）+ `SmsContentObserver`
    │   └── AppBadgeManager.kt          # 全主流国产桌面未读角标管理器与通知权限状态检测
    ├── ui/
    │   ├── PickupViewModel.kt          # 主 UI 状态管理、勾选全选/清除、3秒撤销计时、轻提示、规则增删改排导入导出
    │   ├── components/
    │   │   ├── AppBrandIcons.kt        # `AppleIconSquircleShape` 苹果连续圆角裁剪 & 小组件降采样图标 Bitmap 生成器
    │   │   ├── TaobaoIdentityBanner.kt # 单卡片 / 2~3格「身份码/取件码」紧凑并排模式 & DeepLink 启动器
    │   │   ├── StationGroupCard.kt     # 分组外卡、右上角快捷「+」、左滑视差「已取/删除」卡片
    │   │   └── FloatingPillDock.kt     # 底部悬浮操作栏（单加号 / 对称三按钮 / 3秒撤销 / 分享确认）
    │   ├── home/
    │   │   ├── HomeScreen.kt           # 主页面、`HomeTopHeader` 三态平滑过渡、空状态卡片
    │   │   └── HomeDialogs.kt          # `SpringGlassDialog` 与 6 大交互弹窗
    │   ├── settings/
    │   │   └── SettingsScreen.kt       # 统计卡片、权限状态、1~8号识别规则拖拽/编辑/口令、身份码开关、7天已取弹窗
    │   └── theme/
    │       ├── Color.kt                # Apple HIG 浅/深色调色板 & 7色循环快递房主题色
    │       ├── LiquidGlass.kt          # 120Hz 高帧率预缓存 `liquidGlass`、`liquidGlassInnerCard`、`LiquidGlassBackdrop`
    │       ├── Type.kt                 # 强光等宽特粗取件码排版 `PickupCodeMonospaceStyle` & `AppTypography`
    │       └── Theme.kt                # `PickupCodeTheme(darkTheme)` Light/Dark ColorScheme
    ├── util/
    │   └── PickupShareHelper.kt        # 1080px 高清液态玻璃分享长图 Canvas 渲染与 `FileProvider` 系统分享拉起
    └── widget/
        ├── PickupWidgetProvider.kt     # 桌面小组件刷新、图标 Direct Activity PendingIntent、核销广播
        └── PickupWidgetService.kt      # `RemoteViewsService` 绑定按规则排序的待取列表
```

---

## 3. 视觉设计系统（120Hz Liquid Glass & Apple HIG）

应用整体支持跟随系统深/浅色模式自动切换（Compose 入口读取 `isSystemInDarkTheme()` 切换 `PickupCodeTheme`，`themes.xml` 设 `forceDarkAllowed="false"` 与透明状态栏/导航栏）；桌面应用图标采用 `mipmap-anydpi-v26` 自适应矢量图标（`ic_launcher_foreground.xml` 居中极简快递纸箱线稿，浅色白底炭黑线、深色黑底纯白线）。

### 3.1 调色板（`Color.kt` & `Theme.kt`）
- **主功能色（Light / Dark）**：
  - Blue：`#007AFF` / `#0A84FF`（Scheme `primary`）
  - Green：`#34C759` / `#30D158`（Scheme `secondary`：勾选「确认已取」与成功态）
  - Orange：`#FF9500` / `#FF9F0A`（Scheme `tertiary`：3 秒「撤销已取」与未授权警告）
  - Red：`#FF3B30` / `#FF453A`（Scheme `error`：「删除」按钮与危险操作）
  - Background / Surface：`#EEF2F9` / `#0B0D13`（背景），`#FFFFFF` / `#1A1D27`（表面）
  - OnBackground / OnSurfaceVariant：`#0F172A` / `#F5F7FA`，`#64748B` / `#94A3B8`
- **品牌色**：
  - 淘宝橙渐变：`TaobaoOrangeStart = #FF6A00` -> `TaobaoOrangeEnd = #FF2E00`
  - 菜鸟蓝渐变：`CainiaoBlueStart = #006EFF` -> `CainiaoBlueEnd = #0055D4`
  - 拼多多红渐变：`PinduoduoRedStart = #E12A1F` -> `PinduoduoRedEnd = #C8161D`
- **快递房循环主题色（`stationAccentColor(stationNumber, isDark)`）**：
  - `<= 0`（？ 待分类）：琥珀黄（Light `#D97706` / Dark `#FFD60A`）
  - 按 `(stationNumber - 1) % 7 + 1` 循环映射 7 种高辨识度 Apple 色：
    1. 科技蓝：`#007AFF` / `#0A84FF`
    2. 翡翠绿：`#28B446` / `#30D158`
    3. 活力橙：`#FF8800` / `#FF9F0A`
    4. 靛青紫：`#5856D6` / `#5E5CE6`
    5. 冰川青：`#0099B8` / `#40C8E0`
    6. 霓虹紫：`#9B38D0` / `#BF5AF2`
    7. 珊瑚红：`#E8254D` / `#FF375F`

### 3.2 圆角与 Apple 连续圆角图标（`LiquidGlass.kt` & `AppBrandIcons.kt`）
- **同心嵌套圆角规范（`AppleCorners`）**：
  - `OuterCard = 24.dp`（外部主卡片、弹窗容器、紧凑栏）
  - `InnerCard = 16.dp`（取件码内卡片、输入框、普通按钮、紧凑格内嵌卡）
  - `SmallElement = 12.dp`（标签、Tab 选中滑块、小元素）
  - `Pill = 999.dp`（悬浮胶囊按钮、计数角标、状态标签）
- **应用图标苹果连续圆角（`AppleIconSquircleShape`）**：
  - **严禁**在淘宝/菜鸟/拼多多应用图标外围叠加任何额外白框、圆环、投影底衬或背景边框，直接保持原生正方形图标并按 **iOS Squircle 连续圆角曲率**裁剪（`ContentScale.Crop`）：
    - `r = min(w, h) * 0.2237f`
    - `c = r * 1.28f`（直线切点）
    - `k = r * 0.44f`（三次贝塞尔曲线控制点，实现平滑 G2 曲率过渡）
  - 桌面小组件同样调用 `loadAppleRoundedIconBitmapForWidget` 对 `ic_brand_taobao.webp`、`ic_brand_cainiao.png`、`ic_brand_pinduoduo.png` 按 `inSampleSize` 降采样后使用同一套 Squircle 路径裁成高清 `Bitmap`（96px）。

### 3.3 120Hz 高帧率 Liquid Glass 实现原理
为了保证低/中端及高刷屏上 `LazyColumn` 满帧 120Hz 零卡顿，**禁止**在滑动列表项中使用耗费 GPU 离屏渲染的 `Modifier.blur` 或动态模糊：
1. **`Modifier.liquidGlass(...)`**：使用 `remember(cornerRadius)` 缓存 `RoundedCornerShape`，使用 `remember(isDark, tintColor, surfaceAlphaMultiplier)` 缓存垂直渐变填色 `Brush`（可选从顶部注入 `0.12f/0.18f` 透明度主色调）与 `0.85.dp` 垂直渐变折射边框（顶部高光白、底部柔和暗边框）。
2. **`Modifier.liquidGlassInnerCard(...)`**：采用水平渐变填色 + `0.75.dp` 微弱边框，适配子卡片与待取包裹条。
3. **`LiquidGlassBackdrop`**：使用 `Modifier.drawWithCache` 缓存 4 个静态大半径 `RadialGradient` 光晕画刷，绘制在 `#EEF2F9`（浅） / `#0B0D13`（深）底色之上，零重绘帧率损耗。

### 3.4 排版与防孤儿行规范（`Type.kt`）
- 取件码数字样式 `PickupCodeMonospaceStyle`：`FontFamily.Monospace` + `FontWeight.ExtraBold` + `21.sp` + `letterSpacing = 0.6.sp`。卡片展示时依据码长自动调节字号：`>= 14` 字符用 `18.sp`，`>= 11` 用 `20.sp`，其余用 `22.sp`。
- 全应用所有按钮文字、状态徽章、Tab 标签统一添加 `softWrap = false, maxLines = 1`；长标题、备注、副标题统一 `maxLines = 1, overflow = TextOverflow.Ellipsis`（允许 2 行的引导说明除外），彻底避免出现单字或单标点换行产生的“孤儿行”。

---

## 4. 首页完整界面、交互与动画规范

主页由 `LiquidGlassBackdrop` 包裹，含顶部栏 `HomeTopHeader`、可选身份码入口区、按快递房分组的待取件列表（只展示 `!isPickedUp` 的待取件）与底部悬浮操作栏 `HomeBottomFloatingBar`。

### 4.1 顶部栏 `HomeTopHeader`（三态设计与无 Bug 动画细节）
顶部主行固定高度 `40.dp`，内部所有胶囊按钮高度均为 `36.dp`，包含三种状态 `HomeHeaderMode`：

| 状态 | 触发条件 | 左上角控件 | 右上角控件 | 下方第二行副标题 |
| :--- | :--- | :--- | :--- | :--- |
| **`NORMAL`** | `!isShareSelectionMode && pickupSelectedCount == 0` | 大标题 `取件码`（`28.sp` Bold）+ 未授短信权限时黄色圆点胶囊 `授权短信` | `[转发矢量图标 + 分享取件码]` 胶囊 + `36.dp` 圆形 `⚙` 设置图标按钮（**不显示「设置」二字**） | 待取 > 0：`当前 N 件待取 · 分布在 M 个快递房`（全为未分类则写 `待分类快递房`）；待取 = 0：`每次打开自动扫描最近 7 天取件短信` |
| **`PICKUP_SELECT`**（普通勾选状态） | `!isShareSelectionMode && pickupSelectedCount > 0` | 变为 **`全选`** 胶囊（含 `DoneAll` 图标；全选后变为主题蓝实底白字，未全选为浅蓝底蓝字；点击一键全选或清空待取勾选） | `[分享取件码]` 胶囊（**点击直接打开图片分享弹窗**，沿用当前已勾选条目）+ 最右侧设置齿轮变为 **`取消`** 胶囊（点击清空勾选退出普通勾选态） | `已选 X / Y 件 · 可底部操作或直接分享` |
| **`SHARE_SELECT`**（专门分享选码模式） | 空勾选态下点击 `分享取件码` 进入 | **`全选`** 胶囊（点击一键全选或清空 `shareSelectedIds`） | `[分享取件码]` 平滑水平收起隐藏，最右侧仅保留 **`取消`** 胶囊（点击退出分享选择模式） | `已选 X / Y 件 · 点击卡片或「全选」` |

**右上角「取消」按钮防抖动动画核心要点（v2.3.2 修复点）**：
- 右侧容器使用普通 `Row(verticalAlignment = Alignment.CenterVertically)`，**禁止**在外层 `Row` 叠加 `Modifier.animateContentSize` 或 `Arrangement.spacedBy(8.dp)`，防止同时触发多重宽/间距竞争动画导致「取消」按钮跳动。
- `[分享取件码] + Spacer(8.dp)` 整体包裹在 `AnimatedVisibility(visible = headerMode != SHARE_SELECT)` 中：入场 `fadeIn(180ms) + expandHorizontally(expandFrom = Alignment.End, 200ms, FastOutSlowInEasing)`；退场 **`fadeOut(100ms)` 先于 `shrinkHorizontally(shrinkTowards = Alignment.End, 200ms, FastOutSlowInEasing)`** 完成，避免文字被水平裁剪。
- 最右侧的 `⚙ / 取消` 使用独立的 `AnimatedContent(targetState = headerMode == NORMAL, contentAlignment = Alignment.CenterEnd)`，配合 `SizeTransform(clip = false, 200ms)` 与 `fadeIn(180ms, delay=20ms) + scaleIn(0.88f)` / `fadeOut(110ms) + scaleOut(0.88f)`，保证右边缘全程牢牢锚定在屏幕右上角原地切换。
- 点击顶栏任何操作按钮或切换模式时，自动将首页当前左滑展开卡片状态 `activeSwipedItemId` 置为 `null`。
- `BackHandler` 优先级：处于设置页时按返回退出设置页；处于 `SHARE_SELECT` 时按返回退出分享模式；处于 `PICKUP_SELECT` 时按返回清空当前普通勾选。

### 4.2 身份码 / 取件码快捷入口（`IdentityCodeBannersSection`）
设置页中有 3 个独立开关：淘宝身份码（默认开启）、菜鸟身份码（默认关闭）、拼多多取件码（默认关闭）。
1. **开启 0 个**：隐藏入口区块。
2. **开启 1 个（完整单栏 Banner）**：
   - `liquidGlass(OuterCard, tintColor = brandPrimary)`，水平内边距 `18.dp`、垂直 `14.dp`，按压缩放 `0.97f`。
   - 左侧 `AppBrandIcon(46.dp)` + 标题 + 浅底 `扫码出库` 小标签 + 副标题（如 `一键直达淘宝驿站身份码`）+ 右侧品牌渐变 `打开 >` 按钮。
3. **开启 2 个或 3 个（紧凑并排模式 `CompactIdentityCodeBar`）**：
   - 整个外框 `liquidGlass(OuterCard)`，左上角统一显示小标题 **`身份码/取件码`**（`16.sp` Bold）。
   - 下方为一行等宽（`Modifier.weight(1f)`，间距 `12.dp`）的 **2～3 格 `68.dp` 液态玻璃内嵌方块**，每格内部**只居中使用对应尺寸为 `48.dp` 的原生苹果圆角 `AppBrandIcon`**，没有任何附加文字、副标题或多余边框，最大程度释放首屏垂直空间。按压缩放 `0.94f`。
4. **Exact DeepLink 跳转与容错逻辑**：
   - 淘宝：优先 `taobao://m.taobao.com/tbopen/index.html?h5Url=https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code`，失败尝试菜鸟地址，指定包名 `com.taobao.taobao`。未安装时顶部胶囊提示 `"未检测到淘宝客户端，请先安装手机淘宝后再试"`。
   - 菜鸟：优先 `tbopen://m.taobao.com/tbopen/index.html?h5Url=https://market.m.taobao.com/app/cn-yz/multi-activity/authCode.html`，失败尝试淘宝地址，指定包名 `com.taobao.taobao`。
   - 拼多多：`pinduoduo://com.xunmeng.pinduoduo/mdkd/package`，指定包名 `com.xunmeng.pinduoduo`（异常再尝试不指定包名调起）。未安装时提示 `"未检测到拼多多客户端，请先安装拼多多后再试"`。

### 4.3 快递房分组卡片与快捷 `+` 添加（`StationGroupCard`）
- **列表分组排序**：所有 `stationNumber == 0` 的待分类记录作为第一组；其余快递房严格遵循用户在设置页调整后的 `stationRules` 顺序排列。空列表时展示 `EmptyPickupStateCard`（有短信权限提示点击 `+` 添加，无权限引导授权短信）。
- **分组头部布局**：
  - 左侧：`StationBadgeSymbol(22.dp)`（`1..20` 渲染 `①~⑳`，`0` 渲染 `？`，`badgeNumber == null` 渲染 `17.dp` 实心圆）+ 快递房名称（待分类时下方显示小字 `点击右侧竖点按钮指定快递房`）。
  - 右侧：`Arrangement.spacedBy(6.dp)` 横排 **`[N 件]`** 实底白色字胶囊 + **（非分享模式下显示的）`28.dp` 同色系半透明圆形 `+` 按钮**。
  - **快捷直达添加交互**：点击任意快递房卡片右上角的 `+` 按钮，立即打开「添加取件码」弹窗并传入该卡片对应的 `stationNumber` 作为 `initialStationNumber`：
    - 弹窗标题旁展示当前目标快递房彩色小徽章；
    - 「手动输入取件码」页签水平条用 `LazyRow` 自动滚动定位并高亮选中该快递房；
    - 切换至「粘贴短信解析」页签粘贴短信并入库时，通过 `preferredStationNumber` 强制将该次解析出的记录直接归入当前快递房。

### 4.4 取件码卡片与优雅左滑手势（`SwipeablePickupItemCard`）
- **组内排序**：使用 `PickupItemEntity.NaturalCodeComparator` 将取件码内全部连续数字提取为 `Long` 逐段比对（例如 `1-4-10` < `2-1-5` < `30-3-2` < `100-1-1`），确保货架顺序符合线下实际取货路线。
- **左滑快捷操作物理动效**：
  - 前景卡通过 GPU `graphicsLayer { translationX = offsetX.value }` 横向滑动（120Hz 零重排），最大揭露宽度 `140.dp`；超出 `140.dp` 后进入橡皮筋阻尼（位移乘以 `0.25f`，最远 `1.25 * openRevealPx`）。
  - 底层右侧隐藏两枚与前景等高、`AppleCorners.InnerCard` 圆角的对称大按钮：`62.dp` 绿色「已取」与 `62.dp` 红色「删除」（间距 `8.dp`）。随左滑进度 `progress = revealPx / openRevealPx` 动态呈现：
    - 透明度 `actionAlpha = ((progress - 0.06f) / 0.48f).coerceIn(0f, 1f)`
    - 缩放 `buttonScale = 0.78f + 0.22f * progress`
    - 错位视差位移：「已取」`translationX = remainPx * 0.58f`，「删除」`translationX = remainPx * 0.26f`。
  - 撒手判定：若滑动速度 `velocity < -420f` 或位移过 `0.44x` 则惯性弹簧展开；反之回弹闭合（`spring(dampingRatio = 0.84f, stiffness = 460f, initialVelocity = velocity)`）。
  - 单互斥展开管理：`HomeScreen` 仅记录一个 `activeSwipedItemId`，滑动另一卡片、列表开始滚动、点击顶栏、进入分享模式或点击卡片自身空白处时平滑收起。
  - **注意**：仅在 `!isShareSelectionMode` 时启用左滑；在分享模式下点击卡片任意非菜单区域即可直接切换勾选状态。
- **卡片内部元素（从左到右，无「已到 N 天」角标）**：
  1. 左侧 `28.dp` 圆形勾选框（普通选中填充绿色 + `checkScale = 1.08f` 弹簧缩放；分享模式填充主题蓝；点击即切换选中状态）。
  2. 中部纵向 `Column`：上方为等宽特粗单行 `item.code`；下方 `4.dp` 为可点击商品备注栏（未备注显示主题色「点击备注商品 + 编辑图标」，已备注显示黑色/深色商品名称 + 编辑图标）。
  3. 右侧 `36.dp` 圆形 `⋮` 竖点按钮（点击弹出 `RawMessageDetailDialog` 查看完整短信与修改归属快递房）。
  - 长按卡片（`onLongClick`）弹出 `LongPressQuickActionDialog` 快捷选择「已取」或「删除」。

### 4.5 底部悬浮交互栏（`HomeBottomFloatingBar`）
悬浮于主页底端（距底导航栏 `16.dp`），左右内边距 `20.dp`，圆形按钮统一 `60.dp`，带 `12.dp` 同色投影与 `1.dp` 半透明白边：
- **默认 / 无勾选且无撤销**：仅右下角展示主题蓝 `+` 圆形按钮。
- **普通勾选状态（`!isShareSelectionMode && selectedCount > 0`）**：严格呈现**左右对称三按钮布局**：
  - 左：红色垃圾桶 `60.dp` 圆形按钮（批量删除当前勾选待取记录）。
  - 中：预留 `72.dp` 左右安全边距避免与圆形按钮重叠，显示绿色「确认已取 (N)」胶囊按钮。
  - 右：主题蓝 `+` `60.dp` 圆形按钮（全局快速添加）。
- **3 秒「撤销已取」态**：无论通过勾选点底部「确认已取」、左滑点绿色「已取」还是长按弹窗点「已取」，已取包裹立即从待取列表移除，同时底部中央同位切换为橙色「撤销已取」胶囊按钮并启动 **3000ms 计时器**，3 秒内点击即可恢复该批包裹并弹出胶囊提示。
- **分享选择模式（`isShareSelectionMode`）**：隐藏左右圆形按钮；中央未选码时展示液态玻璃提示胶囊 `请勾选要分享的取件码`，已选 ≥ 1 件时展示主题蓝 `分享已选 (N)` 胶囊按钮。

### 4.6 中心弹性弹窗（`HomeDialogs.kt`）
全部弹窗外层统一采用 `SpringGlassDialog`：屏幕正中心 `TransformOrigin.Center` 从 `0.90f -> 1f` 无过冲弹簧放大（`DampingRatioNoBouncy, StiffnessMedium`）+ `140ms` 淡入，支持 `imePadding()`。

1. **`RawMessageDetailDialog`**：顶部显示快递房徽章、大字取件码、创建时间与关闭 `×`；中间展示「原始短信全文」文本框及横向滚动的「归属快递房」快速切换胶囊；底部左侧红色「删除」，右侧「备注商品」与「完成」。
2. **`EditProductRemarkDialog`**：单行输入商品备注（限 30 字），若原备注非空则底部额外提供「清空备注」文字按钮。
3. **`AddOrPastePickupDialog`**：固定最小高度 `248.dp`，仅含两个页签 **`手动输入取件码`** 与 **`粘贴短信解析`**（默认选中前者），支持从特定快递房 `+` 打开时传入 `initialStationNumber`：
   - 页签 1：输入取件码（限 20 字）、`LazyRow` 选择快递房（自动滚动至预设快递房；若从待分类打开则首项附带 `？ 待分类`）、选填商品备注（限 24 字）。
   - 页签 2：输入/粘贴短信全文（`4~5` 行高）；入库时若有 `preferredStationNumber`，直接覆盖归入该快递房。
4. **`LongPressQuickActionDialog`**：居中大字展示取件码与备注（如有），下方并排等宽绿色「已取」与红色「删除」大按钮。
5. **`NotificationAndBadgePermissionDialog`**：首次引导弹窗，展示「通知权限（开启通知）」与「桌面图标角标（去设置）」状态行与行动按钮。
6. **`SharePickupMethodDialog`**：
   - **仅保留「以图片分享」卡片**，标题为 `以图片分享`（加载时显示 `正在准备清单图片...`），**卡片内不再展示下方的「生成高清分组清单长图……」这行小字**。
   - 卡片下方紧接一行可勾选开关按钮：左侧 `22.dp` 圆形复选框，未勾选时（默认）显示 **`不分享备注`**（右侧灰字 `图片中不附带备注`），勾选后显示 **`分享备注`**（右侧灰字 `图片中附带商品备注`）。
   - 点击「以图片分享」后在 `Dispatchers.IO` 调用 `PickupShareHelper.launchImageShare` 绘制 `1080px` 宽高清液态玻璃 PNG 长图（头部展示 `待取快递清单`、时间、总件数胶囊；各快递房卡片内按自然顺序展示取件码，仅在 `includeRemarks && productName.isNotBlank()` 时绘制备注且动态调整行高 `76f` / `114f`；**不绘制任何「已到 N 天」角标**；底部 watermark `取件码 · 纯本地离线生成`），清理旧缓存并存入 `cacheDir/shared_images/`，通过 `FileProvider` 附带 `ClipData.newUri` 与 `FLAG_GRANT_READ_URI_PERMISSION` 调起系统分享弹窗（可直接发送微信）。分享成功后自动退出分享模式或清空普通勾选态。

---

## 5. 设置页与功能细节（`SettingsScreen.kt`）

从主页向右滑动/渐显打开设置页（`tween(180ms, FastOutSlowInEasing)`：主页透明渐隐并左移 `24.dp`，设置页渐显并从右 `32.dp` 归位）。

1. **顶部栏**：左侧 `36.dp` 圆形浅灰底返回按钮（内部以 `Canvas` 在圆心严格对称绘制 `<` 符号），右侧静态展示「设置」标题。
2. **统计与本地隐私卡片**：
   - 顶部绿色锁头图标 + `100% 纯本地离线存储 · 零联网权限`。
   - 下方 3 格统计卡：**待取包裹**、**7天内已取**（可点击弹出 `PickedUpHistoryDialog` 列表，按取件时间倒序展示，每条支持「恢复」回待取列表或彻底「删除」）、**覆盖快递房**。
3. **权限卡片**：未开短信权限时展示「监听短信权限未开启 -> 开启权限」；另常驻通知与角标状态卡片（未开启显示「开启通知」，已就绪显示「管理角标」一键跳转系统应用通知管理页面）。
4. **识别规则卡片（支持默认 1～8 号、长按拖动排序与口令分享）**：
   - 标题栏右侧提供 `添加快递房` 蓝胶囊与 `重置` 灰胶囊（重置即清除 DataStore 恢复默认 1～8 号）。
   - 下方提供并排 `导出规则口令` / `导入规则口令`（导出格式为 `【取件码识别规则】` + JSON 数组并自动写入系统剪贴板；导入支持识别并校验任意含 JSON 数组的口令文本）。
   - 规则列表支持 **`detectDragGesturesAfterLongPress`** 长按整行平滑拖拽重排（`scale = 1.03f` + 跨越行高 `0.55f` 时交换并触发轻震动反馈，松手即持久化排序并自动重算首页分组顺序与重分类待取件）。
   - 每行右侧配置圆形笔按钮（编辑弹窗内可修改显示名称、数字序号 `1~20` 或留空为实心圆、新增或点击 `×` 删除匹配关键词，点击「保存」即使文本框有未点添加的关键词也会一并自动收纳保存，并立即调用 `reapplyStationRules`）及圆形删除按钮（删除后原先属于该号房的待取件按新规则重分类或进入待分类）。
5. **身份码跳转链路卡片**：独立控制「淘宝身份码」、「菜鸟身份码」、「拼多多取件码」三大开关，每个开关行左侧均使用原生苹果圆角 `AppBrandIcon(40.dp)`。

---

## 6. 智能短信解析、数据持久化与去重引擎

### 6.1 默认 1～8 号识别规则（`SettingsDataStore.kt`）
- 存储键：`KEY_STATION_RULES_JSON = stringPreferencesKey("station_rules_json_v4")`。
- **`defaultStationRules()`**：默认生成编号 **`1..8`** 共 8 个快递房，名称固定为 **`1号快递房` ~ `8号快递房`**，`badgeNumber = 1..8`，并自动预置匹配关键词（以 `num` 为例）：
  `["${num}号快递房", "${num}号房", "${num}号驿站", "${num}号快递站", "${num}号菜鸟"]` + 中文数字（一～十）`["${cn}号快递房", "${cn}号房", "${cn}号驿站"]`。

### 6.2 短信解析流程（`PickupMessageParser.kt`）
1. **前置过滤（`passesPreFilter`）**：
   - 长度 `< 6` 直接丢弃。
   - 命中验证码黑名单（`验证码`、`动态码`、`校验码`、`请勿泄露`、`登录密码`、`修改密码`、`安全码`、`身份验证`）或已完成/营销黑名单（`已签收`、`已出库`、`已代收完成`、`已取件`、`已被取走`、`超时收费`、`退换货`、`优惠券`、`直播间`、`直播中`）直接丢弃。
   - 必须包含取件强特征词之一（`取件码`、`提货码`、`取货码`、`取件编号`、`包裹码`、`开箱码`、`凭`、`快递`、`驿站`、`号房`、`菜鸟`、`丰巢`、`兔喜`、`妈妈驿站`、`近邻宝`、`熊猫快收`、`速递易`、`快递柜`、`到站`、`待取`、`代收点`）。
2. **干扰数字掩码**：提取编码前先依次用空格替换时间段（`08:30-21:00`）、日期（`2026-11-17` / `11月17日`）、手机号/座机/客服号（`138...`、`95xxx`、`400-...`、`100xx`）、手机尾号及数量词（`元/kg/件/号房/号柜/栋/层`等）。
3. **双策略全局取码（`extractPickupCodes`）**：
   - **策略 A**：`CONTEXTUAL_BLOCK_REGEX` 匹配 `取件码/提货码/取货码/包裹码/开箱码/凭码/凭` 后的连续编码片段（支持换行后紧接取件码，及 `、,，/或及和与` 或空格分隔的多码拆分）。
   - **策略 B**：`HYPHENATED_CODE_REGEX` 全局扫描标准货架横杠格式（如 `1-2-3045`、`102-2-1048`、`J8-5-3012`、`A12-3098`）。
   - 统一经 `isValidPickupCodeToken` 校验：总长 `4~18`，至少含 2 个数字，剔除 11 位 1 开头手机号及 `202x/203x-MM-DD` 日期串，并全部转大写后按原序去重。
4. **站房匹配（`matchStation`）**：
   - 优先遍历用户配置的关键词列表（按**关键词字符长度降序**最长匹配）。
   - 未命中关键词则走内置号房兜底正则（支持 `(\d{1,2}|[一-十]) 号 快递房/房/驿站/快递站/菜鸟/快递柜/丰巢/点/门` 及反向写法），且**仅匹配当前规则中已启用（存在于 `stationRules` 列表内）的编号（按 `badgeNumber` 或 `stationNumber` 查表）**——若用户删除了某号房，该号房即归为 `0`（待分类）而非误归类。
5. **商品备注提取（`extractProductName`）**：仅提取明确如 `您购买的【...】`、`订单商品「...」`、`商品名称：...` 的商品描述，绝不提取快递品牌或站点名。

### 6.3 仓储去重、7 天生命周期与并发安全（`PickupRepository.kt`）
- **历史扫描与定时清理**：每次冷/热启动主页 `ON_RESUME` 时，若有短信权限即查询 `Telephony.Sms.Inbox.CONTENT_URI` 最近 7 天短信（`DEFAULT_SMS_SCAN_DAYS = 7`），同时清理早期遗留示例数据（`deleteLegacyDemoItems`）并物理删除 `pickedUpAt`（兜底 `updatedAt`/`createdAt`）早于 **7 天（`7 * 24h`）** 的已取记录（`deleteExpiredPickedUp`）。
- **并发与幂等去重**：`ingestRawMessage` 全程包裹在 `Mutex`（`ingestMutex.withLock`）中执行，以 `code` 不区分大小写查询近 7 天已有记录：
  - 若已有同号房记录、或任一方为待分类 `0` 号：
    - 若已有记录 **已被标记为已取（`isPickedUp == true`）**，则**直接跳过绝不重新恢复为待取**（避免滑走/确认已取的快递再次因扫描 7 天短信复活）。
    - 若未取，则合并补全更确定的站房号（仅当旧站房为 `0` 时升格）、商品备注与更完整原文。
  - 若外部传入 `preferredStationNumber != null`（例如用户从首页某个快递房卡片右上角 `+` 粘贴短信），则直接将本次全部解析结果的站房覆写为 `preferredStationNumber`。

---

## 7. 首次启动权限流水线、桌面角标与桌面小组件

### 7.1 权限引导流水线（`MainActivity.kt`）
1. 等待 `SettingsDataStore` 加载完成（`isSettingsLoaded == true`）后仅触发一次：
2. 检查缺失权限：未授予 `READ_SMS` 则加入 `READ_SMS`、`RECEIVE_SMS`；Android 13+（`TIRAMISU`）未授予 `POST_NOTIFICATIONS` 且（尚无短信权限或尚未引导过角标）则加入 `POST_NOTIFICATIONS`；一次性调起系统权限授权弹窗 `startupPermissionLauncher`。
3. 授权流程结束后，若 `!settings.badgePermissionPrompted`，自动弹出 `NotificationAndBadgePermissionDialog` 引导用户确认开启通知与前往系统开启桌面角标权限（点击关闭或跳转均调用 `markBadgePermissionPrompted` 记录完成状态）。
4. 若未有缺失系统权限且 `!settings.badgePermissionPrompted`，直接弹出 `NotificationAndBadgePermissionDialog`。

### 7.2 多桌面引擎角标（`AppBadgeManager.kt`）
每当 `items` 或 `stationRules` 变化或应用回到前台时，以当前待取件数 `pendingCount` 调用 `updateBadgeCount`：
- 确保注册低重要度静音无震动通知渠道 `pickup_badge_channel`（`setShowBadge(true)`）。
- 同时并行派发各大厂商桌面接口（均做 `try-catch` 防兼容性崩溃）：
  - **华为 / 荣耀**：`ContentResolver.call(content://com.huawei|hihonor.android.launcher.settings/badge/, "change_badge", bundle)`
  - **vivo (OriginOS)**：`sendBroadcast("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM")`（带 `FLAG_RECEIVER_INCLUDE_BACKGROUND = 0x01000000`）
  - **OPPO (ColorOS)**：`ContentResolver.call(content://com.android.badge/badge, "setAppBadgeCount")`
  - **三星 / 通用桌面**：`sendBroadcast("android.intent.action.BADGE_COUNT_UPDATE")`
  - **小米 (HyperOS / MIUI) 与原生 Android**：有 `POST_NOTIFICATIONS` 且 `count > 0` 时发送静音通知（通过反射 `extraNotification.setMessageCount(count)` 适配小米；设 `setNumber(count)` 与 `BADGE_ICON_SMALL`）；`count <= 0` 时立即 `nm.cancel(BADGE_NOTIFICATION_ID)` 清除角标。

### 7.3 桌面液态玻璃小组件（`PickupWidgetProvider` & `PickupWidgetService`）
- **布局与外观**：`4×3`（`250dp × 160dp`，支持水平/垂直拉伸至 `600dp × 900dp`）圆角 `24dp` 磨砂渐变外框（支持浅/深色 `values`/`values-night` 资源），`ListView` 包裹在 `FrameLayout(0dp, weight=1)` 内；内部列表行 `widget_item_root` 采用固定 `56dp` 行高（`14dp` 浅层圆角背景）并统一使用 `dp` 字号（站房 `11dp`、取件码 `17dp`、`已取` 胶囊 `11dp`）及 `TypedValue.COMPLEX_UNIT_DIP` 显式锁定。
- **防拉伸缩放机制**：实现 `onAppWidgetOptionsChanged` 回调刷新 `RemoteViews` 与 `ListView`；Android 12+（`API >= 31`）使用 `RemoteViews(Map<SizeF, RemoteViews>)` 响应式尺寸映射（读取 `OPTION_APPWIDGET_SIZES` 或默认断点），避免定制桌面在纵向拉伸组件高度时等比放大行高与字号。
- **顶部三枚身份码直达图标**：使用 `loadAppleRoundedIconBitmapForWidget`（`fitCenter` `72px` 降采样）生成三枚 `28dp` 原生苹果连续圆角图标（淘宝、菜鸟、拼多多）。为绕过 **Android 12+ 后台启动 Activity 限制**，优先绑定直接 `PendingIntent.getActivity(...)` + `setPackage(...)` 唤起对应客户端；若未检测到包名，则回退绑定 `PendingIntent.getBroadcast` 在接收器中提示 `"未检测到...客户端"`。
- **桌面交互与数据联动**：点击小组件左上角标题区或空视图打开主页面；点击列表行「已取」直接在小组件后台 `goAsync()` 核销该条目并即时同步 `AppBadgeManager` 与刷新全部小组件列表；数据在主页或 `SmsReceiver` 发生任何变更后亦主动调用 `PickupWidgetProvider.notifyWidgetDataChanged(context)`。
