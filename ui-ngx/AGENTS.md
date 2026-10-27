# AGENTS.md

ThingsBoard CE 前端（`ui-ngx`），由 ThingsBoard 后端托管的 Angular 单页应用。

本分支（`feat/ztxk`）是**私有化部署的定制分支**，品牌名为
**中国中铁物联网平台**（China Railway IoT Platform）。修改前请先阅读
「Fork 定制（私有化部署）」章节。

## 技术栈

- Angular 20.3 / Angular Material 20 / CDK 20
- TypeScript 5.9、RxJS 7.8、NgRx 20
- 样式：SCSS + Tailwind CSS；组件选择器前缀 `tb`

## 包管理器（重要）

使用 **Yarn 1.22.22** 与 Node **v22.22.2**。**不要使用 `npm install`** —— 它会因
`ngx-hm-carousel@19` 声明 `@angular/common@^19` 而本项目使用 Angular 20 报
`ERESOLVE` 错误。`package.json` 中的 `resolutions` 字段是 Yarn 专有，npm 会忽略。

```bash
yarn install
```

`prepare` 会执行 `patch-package`，因此 `node_modules` 中的本地依赖文件会被打补丁。

## 常用命令

| 命令 | 用途 |
|------|------|
| `yarn install` | 安装依赖（需要 Yarn 1.x） |
| `yarn start` | 开发服务器，运行于 `http://localhost:4200`（代理到后端 `:18080`） |
| `yarn build` | 开发构建 |
| `yarn build:prod` | 生产构建（`--max_old_space_size=4096`） |
| `yarn lint` | ESLint（`ng lint`） |
| `yarn build:types` | 重新生成 TypeScript 类型（`generate-types.js`） |
| `yarn build:icon-metadata` | 重新生成图标元数据 |

## 本地运行

开发服务器会把 API 请求代理到正在运行的 ThingsBoard 后端：

- `proxy.conf.js` 将 `http://localhost:4200` 转发到 `http://localhost:18080`
  （WebSocket 为 `ws://localhost:18080`）。这针对的是本地 Docker 后端；若要使用默认端口
  的后端，请把 `forwardUrl` / `wsForwardUrl` 改回 `http://localhost:8080`。
- 先启动后端（见根目录 `pom.xml` / `application` 模块），再执行 `yarn start`。

## Fork 定制（私有化部署）

相对上游的主要差异。合并上游改动时请勿“还原”这些定制。

### 品牌与主题

- Logo（品牌蓝 `#195DA2`）：`src/assets/crecg_logo_title_blue.svg`（完整字标）与
  `src/assets/crecg_logo_blue.svg`（方形图标）。
  - 引用位置：`shared/components/logo.component.ts`（登录页默认）、
    `modules/home/home.component.ts`（`logo` / `collapsedLogo`）、
    `modules/home/components/dashboard-page/dashboard-page.component.ts`
    （`defaultDashboardLogo`）。
- 登录页（`modules/login/pages/login/login.component.{html,scss}`）：
  - 整屏背景 `assets/login-background.png` + 深色遮罩；浅色卡片。
  - 所有强调色使用 `#195DA2`（提交/文本/描边按钮、输入框聚焦、图标、进度条、
    分隔线、标题）。
  - 可见标题使用 i18n 键 `login.login-title`。
- 浏览器标签页：
  - Favicon：`index.html` 指向 `assets/crecg_logo_blue.svg`。
  - 静态标题在 `index.html`；运行时标题来自 `env.appTitle` = `中国中铁物联网平台`
    （`src/environments/environment.ts`、`environment.prod.ts`）。`title.service.ts` 渲染为
    `` `${env.appTitle} | ${页面标题}` ``。

### 移除 / 中和的外链

- `shared/models/constants.ts`：`helpBaseUrl = ''`。
- `shared/components/help.component.html` 有意置空，因此所有 `[tb-help]` 的 “?” 按钮
  都不再渲染。请保留 `HelpComponent` 的声明（模板仍绑定 `[tb-help]`）。
- 登录页 logo 链接与 Dashboard 的 “Powered by” 链接已去链接化。
- 删除了 GitHub 徽章组件/模块与 `core/http/git-hub.service.ts`（首页工具栏、Dashboard
  工具栏、部件编辑器中的使用已移除）。
- 设备连接帮助弹窗、IoT Hub 弹窗、移动应用配置弹窗、通知收件人弹窗、首页部件
  （`doc-links`、`getting-started`、`version-info`）以及 locale 帮助提示中硬编码的
  `thingsboard.io` / Flutter / Microsoft 链接均已移除。

### 隐藏的功能与路由

- 隐藏方式：删除菜单引用**并且**解除懒加载模块挂载；功能代码保留。
- 移动中心：从 `menu.models.ts` 移除 `mobile_center`，并从
  `modules/home/pages/home-pages.module.ts` 移除 `MobileModule`。
- IoT Hub：移除 `iot_hub` 菜单项，从 `home-pages.module.ts` 移除 `IotHubModule`，
  并删除租户首页的 IoT Hub 横幅部件。`IotHubActionsService` 的“从 IoT Hub 添加”表格
  操作与全部 `iot-hub` 代码仍保留（未挂载）。

### 当前通过 menuFilters 隐藏的菜单

以下菜单在 `src/app/core/services/menu.models.ts` 的 `menuFilters` 中返回 `false`，
因此不可见，但菜单定义、页面、路由和业务代码均保留：

- 资产：`MenuId.assets`，路径 `/entities/assets`
- 资产配置：`MenuId.asset_profiles`，路径 `/profiles/assetProfiles`
- 实体视图：`MenuId.entity_views`，路径 `/entities/entityViews`
- Edge 管理：`MenuId.edge_management`
- Edge 实例：`MenuId.edges`
- 规则链模板：`MenuId.rulechain_templates`

说明：

- 与移动中心、IoT Hub 的“删除菜单引用并解除模块挂载”不同，以上菜单只做
  “不可见”处理，直接访问对应 URL 仍然可用。
- 如需恢复，删除 `menuFilters` 中对应的 `() => false` 条目即可。
- 如需彻底禁用，需要额外移除路由或模块挂载，并同步更新本文档。

### 首页仪表盘

- `src/assets/dashboard/{sys_admin,tenant_admin,customer_user}_home_page.json` 已定制：
  移除了 Documentation / Version / Connect mobile app（含 Getting started）、SYS_ADMIN
  的 “Functions” 卡片以及 TENANT_ADMIN 的 IoT Hub 横幅；默认布局重排为双列网格；
  `mobileHide` 标记保持。
- 移除部件的方式：删除 `configuration.widgets` 中的条目、`configuration.states.<state>.layouts.main.widgets`
  中的条目，以及任何因此成为孤儿的 `state`。

### 国际化（i18n）

- `src/assets/locale/locale.constant-zh_CN.json` 与 `en_US` 完全对齐（无缺失键）。
  新增任何 `en_US` 键时**必须**同时补充 `zh_CN` 翻译。
- locale 帮助提示中的外链已移除（保留周边文字）。

### Git / IDE

- `.vscode/` 已在根目录 `.gitignore` 与 `ui-ngx/.gitignore` 中忽略。
- 开发工作在 `feat/ztxk` 分支进行。

### 部署注意事项

- 修改 `environment.*`（`appTitle`）、`index.html` 或 SCSS 后，需要重启 `yarn start`
  （环境值在构建时内联）并硬刷新浏览器。
- 由后端/Docker 托管的部署需要执行 `yarn build:prod` 并重新部署；请勿手动修改
  `ui-ngx/target/**` 下的构建产物。

## 架构

详细的路由到源码映射、实体表格/解析器清单、共享组件清单、Angular Material DOM 模式、
表单结构与 HTTP 服务参考见 `structure.md` —— 浏览代码库前请先阅读它。

路径别名（`tsconfig.json`）：

| 别名 | 映射到 |
|------|--------|
| `@app/*` | `src/app/*` |
| `@core/*` | `src/app/core/*` |
| `@modules/*` | `src/app/modules/*` |
| `@home/*` | `src/app/modules/home/*` |
| `@shared/*` | `src/app/shared/*` |
| `@env/*` | `src/environments/*` |

关键目录：

```
src/app/
├── core/
│   ├── http/         # HTTP 服务（每种实体一个）
│   └── services/     # 菜单、鉴权、工具
├── modules/
│   ├── home/
│   │   ├── components/   # 实体表格/详情、告警、仪表盘等
│   │   └── pages/        # 页面模块（每个功能一个目录）
│   └── login/
└── shared/
    ├── components/   # 共享 UI 组件
    └── models/       # 接口 / 枚举
```

大多数页面遵循按实体的三文件模式：`<entity>-table-config.resolver.ts`、
`<entity>.component.ts`（继承 `EntityComponent<T>`）、`<entity>-tabs.component.ts`。

## 代码规范

- 缩进：2 个空格、UTF-8、文件末尾换行（`.editorconfig`）；JSON 使用 4 个空格。
- 除非被要求，否则不要添加注释。
- 使用独立（standalone）Angular 组件；选择器前缀必须为 `tb`。
- 优先复用已有的共享组件/服务，而非临时实现。
- 实体详情表单继承 `EntityComponent<T>` 并实现 `buildForm` /
  `updateForm` / `updateFormState`；保存时合并 `{ ...entity, ...entityFormValue() }`。
- 不使用 `data-testid` 属性；通过 Material 结构、`formControlName`、标签、
  role 与 `matColumnDef` 进行选择。

## 验证

没有可运行的测试套件（仅有 `src/app/core/auth/auth.service.spec.ts`；没有 karma/jest
配置）。通过以下方式验证改动：

```bash
yarn lint
yarn build:prod
```
