# 取件码

<p align="center">
  <a href="https://github.com/MashiroJKB/pickupcode/releases">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="docs/logo-dark.svg" />
      <source media="(prefers-color-scheme: light)" srcset="docs/logo-light.svg" />
      <img src="docs/logo-light.svg" alt="PickupCode" width="136" />
    </picture>
  </a>
</p>

基于本地短信解析 + 自定义 [识别规则](#识别规则) + 桌面小组件的极简离线取件码管理 Android 应用

纯本地运行，绝不申请联网权限，每次打开自动扫描最近 7 天快递短信，自动过滤验证码与营销信息，按快递房与货架号从小到大整理待取包裹

- **自动归类**

  自动拆分一信多码短信并按快递房归类，同一快递房内按货架号自然升序排列；也可点击右下角 `+` 或任意快递房右上角 `+` 手动录入、粘贴短信直达该快递房

- **快捷取件与历史包裹**

  支持长按单条快捷操作，或勾选左侧圆圈批量管理（顶栏切换为「全选 / 取消」，底栏对称展示红色批量删除与绿色「确认已取」）；误操作支持 3 秒内「撤销已取」，已取包裹永久保存在设置页「历史包裹」中并支持随时恢复

- **身份码与桌面小组件**

  支持同时开启淘宝身份码、菜鸟身份码与拼多多取件码入口（开启 2～3 个自动切换为「身份码/取件码」无框对称圆角图标排布）；提供 4×4 液态玻璃桌面小组件一键核销与跳转

- **图片清单分享**

  支持勾选或进入分享模式生成高清液态玻璃分组长图发送至微信等应用，分享弹窗内可切换「不分享备注 / 分享备注」

## 免责声明

**本项目遵循 [MIT License](/LICENSE) 开源，应用纯本地离线运行且不申请任何网络权限，项目仅供学习交流与日常便捷使用**

## 安装

<p>
  <a href="https://github.com/MashiroJKB/pickupcode/releases">
    <img src="https://img.shields.io/github/v/release/MashiroJKB/pickupcode?label=GitHub%20Release&style=for-the-badge&color=007AFF" alt="GitHub Release" />
  </a>
  <a href="https://mashirojk.lanzoub.com/b00ctlhpta">
    <img src="https://img.shields.io/badge/%E8%93%9D%E5%A5%8F%E4%BA%91-%E5%AF%86%E7%A0%81%2043tf-34C759?style=for-the-badge" alt="蓝奏云" />
  </a>
</p>

- 系统要求：Android 10（API 29）及以上
- 检查更新 / 下载渠道：[GitHub Release](https://github.com/MashiroJKB/pickupcode/releases) ｜ [蓝奏云](https://mashirojk.lanzoub.com/b00ctlhpta)（密码：`43tf`）
- 权限说明：仅申请本地短信读取权限（`READ_SMS` / `RECEIVE_SMS`），无联网、通知或桌面角标权限；Android 15/16 侧载安装首次打开时会自动识别当前系统 ROM 并引导解除「设置限制 / 受限设置」后完成短信授权
- 设置页内支持直接访问「关于」、「检查更新」（GitHub Release / 蓝奏云）与「支持」

## 外观

- **120Hz 液态玻璃风格**：采用通透磨砂分层与高帧率预缓存渲染，弹窗统一从屏幕正中心弹性展开，无冗余文字与孤儿折行
- **深 / 浅色模式自适应**：应用界面、桌面小组件与自适应矢量桌面图标均随系统深/浅色模式自动切换

## 识别规则

新安装首次打开时会询问学校快递房数量并自动生成对应的识别规则（默认 1～8 号）；也可在设置页自由增删快递房（关键词留空自动填充默认匹配关键词）、编辑显示名称与前置圆圈序号（`1~20` 显示 `①~⑳`，留空显示实心圆）、配置关键词并长按拖动调整展示顺序

支持通过剪贴板口令一键导出或导入整套识别规则，方便同校同学之间直接共享驿站配置

<details>
<summary>示例规则口令格式（点击展开）</summary>

```text
【取件码识别规则】[{"stationNumber":1,"displayName":"1号快递房","badgeNumber":1,"keywords":["1号快递房","1号房","1号驿站"]}]
```

</details>

## 开发

- [产品与技术规范](PRD.md)
- 构建 Release 安装包：`./gradlew assembleRelease`
- 运行单元测试：`./gradlew testDebugUnitTest`
