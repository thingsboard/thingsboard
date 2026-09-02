# ThingsBoard CE Frontend — Navigation & Testing Guide

## 1. Quick Reference

| Key | Value |
|-----|-------|
| Angular | 20 |
| Angular Material | 20 |
| Component prefix | `tb` |
| TypeScript | 5.9 |
| RxJS | 7.8 |

**Path Aliases** (from `tsconfig.json`):

| Alias | Maps to |
|-------|---------|
| `@app/*` | `src/app/*` |
| `@env/*` | `src/environments/*` |
| `@core/*` | `src/app/core/*` |
| `@modules/*` | `src/app/modules/*` |
| `@home/*` | `src/app/modules/home/*` |
| `@shared/*` | `src/app/shared/*` |

**Key Directories:**

```
src/app/
├── core/
│   ├── http/                  # HTTP services (one per entity type)
│   └── services/              # Menu, auth, utils
├── modules/
│   ├── home/
│   │   ├── components/        # Reusable home components (entity table, details panel, alarm, dashboard)
│   │   │   └── entity/        # EntitiesTableComponent, EntityDetailsPageComponent, EntityComponent base
│   │   └── pages/             # Page modules (one directory per page/feature)
│   └── login/                 # Login, reset password, 2FA
└── shared/
    ├── components/            # Shared components (autocompletes, dialogs, time, image)
    └── models/                # TypeScript interfaces/enums
```

---

## 2. Route-to-Source Mapping

All page source paths are relative to `src/app/modules/home/pages/`.
Page types: `entity-table` = EntitiesTableComponent + resolver, `entity-details` = EntityDetailsPageComponent, `settings-form` = standalone form with ConfirmOnExitGuard, `dashboard-view` = DashboardViewComponent with resolver, `custom` = unique component.

### Login (unauthenticated)

Source paths below are relative to `src/app/modules/` (not `home/pages/`).

| URL | Source | Type |
|-----|--------|------|
| `/login` | `login/pages/login/` | custom (LoginComponent) |
| `/login/resetPasswordRequest` | `login/pages/login/` | custom (ResetPasswordRequestComponent) |
| `/login/resetPassword` | `login/pages/login/` | custom (ResetPasswordComponent) |
| `/login/createPassword` | `login/pages/login/` | custom (CreatePasswordComponent) |
| `/login/mfa` | `login/pages/login/` | custom (TwoFactorAuthLoginComponent) |

### Home

| URL | Source | Type | Auth |
|-----|--------|------|------|
| `/home` | `home-links/` | custom (HomeLinksComponent + dashboard resolver) | ALL |

### Tenants (SYS_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/tenants` | `tenant/` | entity-table | `TenantsTableConfigResolver` |
| `/tenants/:entityId` | `tenant/` | entity-details | `TenantsTableConfigResolver` |
| `/tenants/:tenantId/users` | `tenant/` + `user/` | entity-table | `UsersTableConfigResolver` |
| `/tenantProfiles` | `tenant-profile/` | entity-table | `TenantProfilesTableConfigResolver` |
| `/tenantProfiles/:entityId` | `tenant-profile/` | entity-details | `TenantProfilesTableConfigResolver` |

### Alarms Center (TENANT_ADMIN, CUSTOMER_USER)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/alarms` | `alarm/` | custom (RouterTabsComponent) | — |
| `/alarms/alarms` | `alarm/` | custom (AlarmTableComponent) | — |
| `/alarms/alarm-rules` | `alarm/` | custom (AlarmRulesTableComponent) | — |
| `/alarms/alarm-rules/:entityId` | `alarm/` | entity-details | `AlarmRulesTableConfigResolver` |

### Dashboards (TENANT_ADMIN, CUSTOMER_USER)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/dashboards` | `dashboard/` | entity-table | `DashboardsTableConfigResolver` |
| `/dashboards/:dashboardId` | `dashboard/` | custom (DashboardPageComponent) | `DashboardResolver` |

### Entities (TENANT_ADMIN, CUSTOMER_USER)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/entities/devices` | `device/` | entity-table | `DevicesTableConfigResolver` |
| `/entities/devices/:entityId` | `device/` | entity-details | `DevicesTableConfigResolver` |
| `/entities/assets` | `asset/` | entity-table | `AssetsTableConfigResolver` |
| `/entities/assets/:entityId` | `asset/` | entity-details | `AssetsTableConfigResolver` |
| `/entities/entityViews` | `entity-view/` | entity-table | `EntityViewsTableConfigResolver` |
| `/entities/entityViews/:entityId` | `entity-view/` | entity-details | `EntityViewsTableConfigResolver` |
| `/entities/gateways` | `gateways/` | dashboard-view | `gatewaysDashboardResolver` |

### Profiles (TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/profiles/deviceProfiles` | `device-profile/` | entity-table | `DeviceProfilesTableConfigResolver` |
| `/profiles/deviceProfiles/:entityId` | `device-profile/` | entity-details | `DeviceProfilesTableConfigResolver` |
| `/profiles/assetProfiles` | `asset-profile/` | entity-table | `AssetProfilesTableConfigResolver` |
| `/profiles/assetProfiles/:entityId` | `asset-profile/` | entity-details | `AssetProfilesTableConfigResolver` |

### Customers (TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/customers` | `customer/` | entity-table | `CustomersTableConfigResolver` |
| `/customers/:entityId` | `customer/` | entity-details | `CustomersTableConfigResolver` |
| `/customers/:customerId/users` | `customer/` + `user/` | entity-table | `UsersTableConfigResolver` |
| `/customers/:customerId/devices` | `customer/` + `device/` | entity-table | `DevicesTableConfigResolver` |
| `/customers/:customerId/assets` | `customer/` + `asset/` | entity-table | `AssetsTableConfigResolver` |
| `/customers/:customerId/dashboards` | `customer/` + `dashboard/` | entity-table | `DashboardsTableConfigResolver` |
| `/customers/:customerId/edgeInstances` | `customer/` + `edge/` | entity-table | `EdgesTableConfigResolver` |

### Calculated Fields (TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/calculatedFields` | `calculated-fields/` | custom (CalculatedFieldsTableComponent) | — |
| `/calculatedFields/:entityId` | `calculated-fields/` | entity-details | `CalculatedFieldsTableConfigResolver` |

### Rule Chains (TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/ruleChains` | `rulechain/` | entity-table | `RuleChainsTableConfigResolver` |
| `/ruleChains/:ruleChainId` | `rulechain/` | custom (RuleChainPageComponent, lazy) | `RuleChainResolver` + others |

### Edge Management (TENANT_ADMIN, conditional on edgesSupportEnabled)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/edgeManagement/instances` | `edge/` | entity-table | `EdgesTableConfigResolver` |
| `/edgeManagement/instances/:entityId` | `edge/` | entity-details | `EdgesTableConfigResolver` |
| `/edgeManagement/instances/:edgeId/assets` | `edge/` + `asset/` | entity-table | `AssetsTableConfigResolver` |
| `/edgeManagement/instances/:edgeId/devices` | `edge/` + `device/` | entity-table | `DevicesTableConfigResolver` |
| `/edgeManagement/instances/:edgeId/entityViews` | `edge/` + `entity-view/` | entity-table | `EntityViewsTableConfigResolver` |
| `/edgeManagement/instances/:edgeId/dashboards` | `edge/` + `dashboard/` | entity-table | `DashboardsTableConfigResolver` |
| `/edgeManagement/instances/:edgeId/ruleChains` | `edge/` + `rulechain/` | entity-table | `RuleChainsTableConfigResolver` |
| `/edgeManagement/ruleChains` | `edge/` + `rulechain/` | entity-table | `RuleChainsTableConfigResolver` |

### Advanced Features (TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/features/otaUpdates` | `ota-update/` | entity-table | `OtaUpdateTableConfigResolve` |
| `/features/otaUpdates/:entityId` | `ota-update/` | entity-details | `OtaUpdateTableConfigResolve` |
| `/features/vc` | `vc/` | custom (VersionControlComponent) | — |

### Resources (SYS_ADMIN, TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/resources/widgets-library/widget-types` | `widget/` | entity-table | `WidgetTypesTableConfigResolver` |
| `/resources/widgets-library/widgets-bundles` | `widget/` | entity-table | `WidgetsBundlesTableConfigResolver` |
| `/resources/widgets-library/widgets-bundles/:widgetsBundleId` | `widget/` | custom (WidgetsBundleWidgetsComponent) | — |
| `/resources/widgets-library/widget-types/:widgetTypeId` | `widget/` | custom (WidgetEditorComponent) | — |
| `/resources/images` | `admin/` | custom (ImageGalleryComponent) | — |
| `/resources/scada-symbols` | `admin/` | custom (ImageGalleryComponent) | — |
| `/resources/scada-symbols/:type/:key` | `scada-symbol/` | custom (ScadaSymbolComponent) | — |
| `/resources/resources-library` | `admin/` | entity-table | `ResourcesLibraryTableConfigResolver` |
| `/resources/resources-library/:entityId` | `admin/` | entity-details | `ResourcesLibraryTableConfigResolver` |
| `/resources/javascript-library` | `admin/` | entity-table | `JsLibraryTableConfigResolver` |
| `/resources/javascript-library/:entityId` | `admin/` | entity-details | `JsLibraryTableConfigResolver` |

### Notifications (ALL authorities)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/notification/inbox` | `notification/inbox/` | entity-table | `InboxTableConfigResolver` |
| `/notification/sent` | `notification/sent/` | entity-table | `SentTableConfigResolver` |
| `/notification/recipients` | `notification/recipient/` | entity-table | `RecipientTableConfigResolver` |
| `/notification/templates` | `notification/template/` | entity-table | `TemplateTableConfigResolver` |
| `/notification/rules` | `notification/rule/` | entity-table | `RuleTableConfigResolver` |

### Mobile Center (SYS_ADMIN, TENANT_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/mobile-center/bundles` | `mobile/bundes/` | entity-table | `MobileBundleTableConfigResolver` |
| `/mobile-center/applications` | `mobile/applications/` | entity-table | `MobileAppTableConfigResolver` |
| `/mobile-center/applications/:entityId` | `mobile/applications/` | entity-details | `MobileAppTableConfigResolver` |
| `/mobile-center/qr-code-widget` | `mobile/qr-code-widget/` | settings-form | — |

### Settings — SYS_ADMIN

| URL | Source | Type |
|-----|--------|------|
| `/settings/general` | `admin/` | settings-form (GeneralSettingsComponent) |
| `/settings/outgoing-mail` | `admin/` | settings-form (MailServerComponent) |
| `/settings/notifications` | `admin/` | settings-form (SmsProviderComponent) |
| `/settings/queues` | `admin/` | entity-table (`QueuesTableConfigResolver`) |

### Settings — TENANT_ADMIN

| URL | Source | Type |
|-----|--------|------|
| `/settings/home` | `admin/` | settings-form (HomeSettingsComponent) |
| `/settings/notifications` | `admin/` | settings-form (SmsProviderComponent) |
| `/settings/repository` | `admin/` | settings-form (RepositoryAdminSettingsComponent) |
| `/settings/auto-commit` | `admin/` | settings-form (AutoCommitAdminSettingsComponent) |
| `/settings/trendz` | `admin/` | settings-form (TrendzSettingsComponent) |
| `/settings/ai-models` | `ai-model/` | entity-table (`AiModelsTableConfigResolver`) |

### Security Settings (SYS_ADMIN)

| URL | Source | Type | Resolver |
|-----|--------|------|----------|
| `/security-settings/general` | `admin/` | settings-form (SecuritySettingsComponent) | — |
| `/security-settings/2fa` | `admin/` | settings-form (TwoFactorAuthSettingsComponent) | — |
| `/security-settings/oauth2/domains` | `admin/` | entity-table | `DomainTableConfigResolver` |
| `/security-settings/oauth2/clients` | `admin/` | entity-table | `ClientsTableConfigResolver` |
| `/security-settings/auditLogs` | `admin/` | custom (audit log table) | — |

### Account (ALL authorities)

| URL | Source | Type |
|-----|--------|------|
| `/account/profile` | `profile/` | settings-form (ProfileComponent) |
| `/account/security` | `security/` | settings-form (SecurityComponent) |

### API Usage (TENANT_ADMIN)

| URL | Source | Type |
|-----|--------|------|
| `/usage` | `api-usage/` | dashboard-view (loads `api_usage.json`) |

---

## 3. Entity Table Pattern

Most pages follow a 3-file pattern per entity type inside `pages/<entity>/`:

```
<entity>-table-config.resolver.ts   — Column definitions, actions, component wiring
<entity>.component.ts + .html       — Entity detail form (extends EntityComponent<T>)
<entity>-tabs.component.ts + .html  — Additional detail tabs (relations, attributes, events, etc.)
```

**Framework files** (in `home/components/entity/`):
- `entities-table.component.ts` — The generic table component. Renders columns, actions, pagination from config.
- `entity-details-page.component.ts` — Full-page entity details with tabs.
- `entity-details-panel.component.ts` — Slide-out drawer panel for entity details.
- `entity.component.ts` — Abstract base directive all entity forms extend.
- `entities-table-config.models.ts` — TypeScript interfaces for table configuration.

**Resolver structure** — each resolver returns an `EntityTableConfig<T>` with:
- `entityType` — enum value (e.g., `EntityType.DEVICE`)
- `columns` — array of column definitions (header key, cell property/template, width)
- `cellActionDescriptors` — per-row action buttons
- `groupActionDescriptors` — bulk-selection action buttons
- `headerActionDescriptors` — toolbar action buttons (add, import)
- `entityComponent` — reference to the detail form component class
- `entityTabsComponent` — reference to the tabs component class
- `addDialogComponent` — optional dialog for add flow (e.g., `DeviceWizardDialogComponent`)

### All Table Config Resolvers

| Resolver | Entity Type | Source Dir | Entity Component | Tabs Component |
|----------|-------------|-----------|------------------|----------------|
| `TenantsTableConfigResolver` | TENANT | `tenant/` | `TenantComponent` | `TenantTabsComponent` |
| `TenantProfilesTableConfigResolver` | TENANT_PROFILE | `tenant-profile/` | `TenantProfileComponent` | `TenantProfileTabsComponent` |
| `DevicesTableConfigResolver` | DEVICE | `device/` | `DeviceComponent` | `DeviceTabsComponent` |
| `DeviceProfilesTableConfigResolver` | DEVICE_PROFILE | `device-profile/` | `DeviceProfileComponent` | `DeviceProfileTabsComponent` |
| `AssetsTableConfigResolver` | ASSET | `asset/` | `AssetComponent` | `AssetTabsComponent` |
| `AssetProfilesTableConfigResolver` | ASSET_PROFILE | `asset-profile/` | `AssetProfileComponent` | `AssetProfileTabsComponent` |
| `EntityViewsTableConfigResolver` | ENTITY_VIEW | `entity-view/` | `EntityViewComponent` | `EntityViewTabsComponent` |
| `CustomersTableConfigResolver` | CUSTOMER | `customer/` | `CustomerComponent` | `CustomerTabsComponent` |
| `UsersTableConfigResolver` | USER | `user/` | `UserComponent` | `UserTabsComponent` |
| `DashboardsTableConfigResolver` | DASHBOARD | `dashboard/` | `DashboardFormComponent` | `DashboardTabsComponent` |
| `EdgesTableConfigResolver` | EDGE | `edge/` | `EdgeComponent` | `EdgeTabsComponent` |
| `RuleChainsTableConfigResolver` | RULE_CHAIN | `rulechain/` | `RuleChainComponent` | `RuleChainTabsComponent` |
| `OtaUpdateTableConfigResolve` | OTA_PACKAGE | `ota-update/` | `OtaUpdateComponent` | `OtaUpdateTabsComponent` |
| `CalculatedFieldsTableConfigResolver` | CALCULATED_FIELD | `calculated-fields/` (config in `home/components/calculated-fields/`) | `CalculatedFieldComponent` | `CalculatedFieldsTabsComponent` |
| `WidgetTypesTableConfigResolver` | WIDGETS_BUNDLE | `widget/` | `WidgetTypeComponent` | `WidgetTypeTabsComponent` |
| `WidgetsBundlesTableConfigResolver` | WIDGETS_BUNDLE | `widget/` | `WidgetsBundleComponent` | `WidgetsBundleTabsComponent` |
| `ResourcesLibraryTableConfigResolver` | TB_RESOURCE | `admin/` | `ResourcesLibraryComponent` | `ResourceLibraryTabsComponent` |
| `JsLibraryTableConfigResolver` | TB_RESOURCE | `admin/` | `JsResourceComponent` | `ResourceTabsComponent` |
| `QueuesTableConfigResolver` | QUEUE | `admin/queue/` | `QueueComponent` | — |
| `AiModelsTableConfigResolver` | AI_MODEL | `ai-model/` | — | — |
| `ClientsTableConfigResolver` | OAUTH2_CLIENT | `admin/oauth2/clients/` | `ClientComponent` | — |
| `InboxTableConfigResolver` | NOTIFICATION | `notification/inbox/` | — (dialog) | — |
| `SentTableConfigResolver` | NOTIFICATION_REQUEST | `notification/sent/` | — (dialog) | — |
| `RecipientTableConfigResolver` | NOTIFICATION_TARGET | `notification/recipient/` | — (dialog) | — |
| `TemplateTableConfigResolver` | NOTIFICATION_TEMPLATE | `notification/template/` | — (dialog) | — |
| `RuleTableConfigResolver` | NOTIFICATION_RULE | `notification/rule/` | — (dialog) | — |
| `MobileAppTableConfigResolver` | MOBILE_APP | `mobile/applications/` | `MobileAppComponent` | — |
| `MobileBundleTableConfigResolver` | MOBILE_BUNDLE | `mobile/bundes/` | — | — |
| `AlarmRulesTableConfigResolver` | CALCULATED_FIELD | `alarm/` (config in `home/components/alarm-rules/`) | — | — |
| `DomainTableConfigResolver` | DOMAIN | `admin/oauth2/domains/` | `DomainComponent` | — |

---

## 4. Shared Component Catalog

### `@shared/components/entity/`

| Selector | Purpose |
|----------|---------|
| `tb-entity-autocomplete` | Autocomplete input for selecting a single entity |
| `tb-entity-select` | Dropdown select for entity |
| `tb-entity-list` | Chip list for multiple entities |
| `tb-entity-list-select` | Dropdown with entity list selection |
| `tb-entity-key-autocomplete` | Autocomplete for entity attribute/telemetry keys |
| `tb-entity-keys-list` | Chip list for multiple entity keys |
| `tb-entity-type-select` | Dropdown for entity type enum |
| `tb-entity-type-list` | Chip list for multiple entity types |
| `tb-entity-subtype-select` | Dropdown for entity subtype (e.g., device type) |
| `tb-entity-subtype-list` | Chip list for multiple subtypes |
| `tb-entity-subtype-autocomplete` | Autocomplete for entity subtype |
| `tb-entity-gateway-select` | Dropdown for selecting a gateway device |

### `@shared/components/time/`

| Selector | Purpose |
|----------|---------|
| `tb-timewindow` | Timewindow selector (realtime/history) |
| `tb-timewindow-panel` | Expanded panel for timewindow config |
| `tb-timewindow-config-dialog` | Dialog for advanced timewindow settings |
| `tb-datetime` | Date-time picker input |
| `tb-datetime-period` | Date range picker |
| `tb-timezone` | Timezone display |
| `tb-timezone-select` | Dropdown for timezone selection |
| `tb-quick-time-interval` | Quick interval preset selector |
| `tb-timeinterval` | Duration input (number + unit) |
| `tb-datapoints-limit` | Numeric input for data point limits |
| `tb-aggregation-type-select` | Dropdown for aggregation type (AVG, SUM, etc.) |
| `tb-history-selector` | History navigation selector |

### `@shared/components/image/`

| Selector | Purpose |
|----------|---------|
| `tb-image-gallery` | Grid gallery of uploaded images |
| `tb-image-gallery-dialog` | Dialog wrapper for image gallery |
| `tb-image-input` | Single image upload input |
| `tb-gallery-image-input` | Image input that opens gallery for selection |
| `tb-multiple-gallery-image-input` | Multi-image input with gallery |
| `tb-upload-image-dialog` | Upload dialog |
| `tb-embed-image-dialog` | Embed external image dialog |
| `tb-scada-symbol-input` | SCADA symbol selector input |

### `@shared/components/dialog/`

| Selector | Purpose |
|----------|---------|
| `tb-confirm-dialog` | Yes/No confirmation dialog |
| `tb-alert-dialog` | Information alert dialog |
| `tb-error-alert-dialog` | Error message dialog |
| `tb-color-picker-dialog` | Color picker in a dialog |
| `tb-material-icons-dialog` | Material icon browser dialog |
| `tb-node-script-test-dialog` | Script testing dialog for rule nodes |
| `tb-object-edit-dialog` | JSON object editor dialog |
| `tb-todo-dialog` | Todo/checklist dialog |

### `@shared/components/` (other)

| Selector | Purpose |
|----------|---------|
| `tb-json-content` | JSON syntax-highlighted viewer |
| `tb-json-object-edit` | Editable JSON object component |
| `tb-json-object-view` | Read-only JSON object display |
| `tb-markdown` | Markdown editor/viewer |
| `tb-breadcrumb` | Breadcrumb navigation bar |
| `tb-icon` | Custom icon component |
| `tb-logo` | ThingsBoard logo |
| `tb-copy-button` | Copy-to-clipboard button |
| `tb-toggle-password` | Password visibility toggle |
| `tb-file-input` | File upload input |
| `tb-phone-input` | International phone input |
| `tb-unit-input` | Measurement unit input |
| `tb-value-input` | Generic value input with type selection |
| `tb-color-input` | Color picker input |
| `tb-color-picker` | Inline color picker |
| `tb-key-val-map` | Key-value pair map editor |
| `tb-nav-tree` | Tree navigation component |
| `tb-user-menu` | User profile dropdown menu |
| `tb-notification-bell` | Notification bell icon with badge |
| `tb-footer` | Page footer |
| `tb-scroll-grid` | Virtual-scroll grid |
| `tb-social-share-panel` | Social sharing buttons |

### Autocomplete / Select Components

| Selector | Purpose |
|----------|---------|
| `tb-dashboard-autocomplete` | Autocomplete for dashboard selection |
| `tb-dashboard-select` | Dropdown for dashboard |
| `tb-dashboard-state-autocomplete` | Autocomplete for dashboard states |
| `tb-country-autocomplete` | Country autocomplete |
| `tb-string-autocomplete` | Generic string autocomplete |
| `tb-message-type-autocomplete` | Rule engine message type autocomplete |
| `tb-queue-autocomplete` | Queue name autocomplete |
| `tb-relation-type-autocomplete` | Relation type autocomplete |
| `tb-resource-autocomplete` | Resource autocomplete |
| `tb-ota-package-autocomplete` | OTA package autocomplete |
| `tb-branch-autocomplete` | Git branch autocomplete |
| `tb-rule-chain-select` | Rule chain select dropdown |
| `tb-widgets-bundle-select` | Widgets bundle select dropdown |
| `tb-template-autocomplete` | Notification template autocomplete |
| `tb-material-icon-select` | Material icon picker |
| `tb-slack-conversation-autocomplete` | Slack conversation autocomplete |

### `@home/components/` (key components)

| Selector | Purpose |
|----------|---------|
| `tb-entities-table` | Main entity table (columns, sort, pagination, actions) |
| `tb-entity-details-panel` | Slide-out panel for entity details |
| `tb-entity-details-page` | Full-page entity details view |
| `tb-add-entity-dialog` | Generic add-entity dialog |
| `tb-entity-filter` | Entity data filter component |
| `tb-entity-chips` | Chip display for entity references |
| `tb-details-panel` | Base details panel (toolbar + edit/apply FABs) |
| `tb-alarm-table` | Alarm list table |
| `tb-alarm-details-dialog` | Alarm detail dialog with ack/clear |
| `tb-alarm-assignee` | Alarm assignee selector |
| `tb-alarm-comment` | Alarm comment thread |
| `tb-alarm-filter-config` | Alarm filter configuration |
| `tb-audit-log-table` | Audit log table |
| `tb-attribute-table` | Entity attribute key-value table |
| `tb-relation-table` | Entity relation table |
| `tb-event-table` | Entity event table |
| `tb-dashboard` | Dashboard renderer |
| `tb-dashboard-page` | Dashboard page (toolbar + dashboard) |
| `tb-dashboard-toolbar` | Dashboard edit/view toolbar |
| `tb-edit-widget` | Widget editor |
| `tb-router-tabs` | Tab navigation via router (used for section tabs) |
| `tb-device-credentials` | Device credentials form |

---

## 5. Angular Material DOM Patterns

**No `data-testid` attributes** — selectors must use Angular Material structure, `formControlName`, `matColumnDef`, labels, roles, and classes.

### Login Form

```html
<mat-card appearance="raised">
  <mat-card-content>
    <form class="tb-login-form" [formGroup]>
      <mat-form-field appearance="outline">
        <mat-label><!-- translated --></mat-label>
        <input id="username-input" matInput type="email" formControlName="username">
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label><!-- translated --></mat-label>
        <input id="password-input" matInput type="password" formControlName="password">
      </mat-form-field>
      <button mat-flat-button type="submit"><!-- Sign In --></button>
    </form>
  </mat-card-content>
</mat-card>
```

Key selectors: `#username-input`, `#password-input`, `button[type="submit"]`.

### Main Layout

```html
<mat-sidenav-container>
  <mat-sidenav id="sidenav" class="tb-site-sidenav">
    <tb-side-menu/>
  </mat-sidenav>
  <mat-sidenav-content>
    <mat-toolbar color="primary" class="tb-primary-toolbar">
      <button id="main" mat-icon-button><!-- menu toggle --></button>
      <tb-breadcrumb/>
      <tb-notification-bell/>
      <tb-user-menu/>
    </mat-toolbar>
    <router-outlet/>
  </mat-sidenav-content>
</mat-sidenav-container>
```

Key selectors: `mat-sidenav#sidenav`, `button#main`, `tb-breadcrumb`, `tb-user-menu`.

### Entity Table

```html
<mat-drawer-container hasBackdrop="false">
  <mat-drawer position="end" class="tb-details-drawer">
    <tb-entity-details-panel/>
  </mat-drawer>
  <mat-drawer-content>
    <div class="tb-entity-table">
      <!-- Toolbar: title, add button, refresh, search -->
      <mat-toolbar class="mat-mdc-table-toolbar">
        <span class="tb-entity-table-title"><!-- title --></span>
        <button mat-icon-button><!-- add --></button>
        <button mat-icon-button><!-- refresh --></button>
        <button mat-icon-button><!-- search toggle --></button>
      </mat-toolbar>
      <!-- Search toolbar (hidden by default) -->
      <mat-toolbar class="mat-mdc-table-toolbar">
        <mat-form-field>
          <input id="searchInput" matInput [formControl]="textSearch">
        </mat-form-field>
      </mat-toolbar>
      <!-- Selection toolbar (shown when rows selected) -->
      <mat-toolbar class="mat-mdc-table-toolbar" color="primary">
        <!-- group action buttons -->
      </mat-toolbar>
      <!-- Data table -->
      <div class="table-container">
        <table mat-table [matSort]>
          <ng-container matColumnDef="select" sticky>
            <mat-header-cell> <mat-checkbox/> </mat-header-cell>
            <mat-cell> <mat-checkbox/> </mat-cell>
          </ng-container>
          <!-- Dynamic columns -->
          <ng-container [matColumnDef]="column.key">
            <mat-header-cell mat-sort-header><!-- header --></mat-header-cell>
            <mat-cell><!-- value --></mat-cell>
          </ng-container>
          <!-- Actions column -->
          <ng-container matColumnDef="actions" stickyEnd>
            <mat-cell>
              <button mat-icon-button><!-- per-row actions --></button>
            </mat-cell>
          </ng-container>
          <mat-header-row sticky="true"/>
          <mat-row/>
        </table>
      </div>
      <mat-paginator/>
    </div>
  </mat-drawer-content>
</mat-drawer-container>
```

Key selectors: `table[mat-table]`, `mat-row`, `mat-cell`, `mat-paginator`, `[matColumnDef="select"]`, `[matColumnDef="actions"]`. Note: the search input uses Angular template ref `#searchInput` (not an HTML `id`); target it via `.tb-entity-table mat-toolbar input[matInput]` in the search toolbar context.

### Entity Details Page

```html
<mat-card appearance="outlined" class="settings-card">
  <mat-toolbar class="details-toolbar">
    <button mat-icon-button><!-- back --></button>
    <div class="tb-details-title"><!-- entity name --></div>
    <section class="tb-header-buttons">
      <button mat-fab color="accent"><!-- apply (done icon) --></button>
      <button mat-fab color="accent"><!-- edit/close toggle --></button>
    </section>
  </mat-toolbar>
  <mat-card-content>
    <mat-tab-group mat-stretch-tabs="false">
      <mat-tab><!-- entity form injected here --></mat-tab>
      <!-- additional tabs: attributes, relations, events, etc. -->
    </mat-tab-group>
  </mat-card-content>
</mat-card>
```

Key selectors: `mat-toolbar.details-toolbar`, `button[mat-fab][color="accent"]`, `mat-tab-group`, `mat-tab`.

### Details Panel (Slide-Out Drawer)

```html
<header>
  <mat-toolbar class="details-toolbar" color="primary">
    <span class="tb-details-title-text"><!-- entity name --></span>
    <button mat-icon-button><!-- close --></button>
    <section class="tb-header-buttons">
      <button mat-fab color="accent"><!-- apply --></button>
      <button mat-fab color="accent"><!-- edit/close --></button>
    </section>
  </mat-toolbar>
</header>
<div class="mat-content">
  <mat-tab-group><!-- tabs --></mat-tab-group>
</div>
```

### Entity Detail Form (inside a tab)

```html
<div class="mat-padding">
  <form [formGroup]="entityForm">
    <fieldset [disabled]="isEntityReadonly">
      <mat-form-field>
        <mat-label><!-- translated label --></mat-label>
        <input matInput formControlName="name">
        <mat-error><!-- validation message --></mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label><!-- translated label --></mat-label>
        <input matInput formControlName="label">
      </mat-form-field>
      <tb-entity-autocomplete formControlName="someEntityId"/>
      <mat-slide-toggle formControlName="gateway"><!-- label --></mat-slide-toggle>
      <mat-form-field>
        <textarea matInput formControlName="description"></textarea>
      </mat-form-field>
    </fieldset>
  </form>
</div>
```

Key selectors: `input[formControlName="name"]`, `mat-slide-toggle[formControlName="..."]`, `textarea[formControlName="description"]`.

### Dialog

```html
<form style="width: 750px;">
  <mat-toolbar color="primary">
    <h2 translate><!-- dialog title --></h2>
    <span class="flex-1"></span>
    <button mat-icon-button><!-- close --></button>
  </mat-toolbar>
  <mat-progress-bar/>
  <div mat-dialog-content>
    <!-- form content -->
  </div>
  <div mat-dialog-actions>
    <button mat-button><!-- Cancel --></button>
    <button mat-raised-button type="submit"><!-- Add/Save --></button>
  </div>
</form>
```

Key selectors: `[mat-dialog-content]`, `[mat-dialog-actions]`, `button[type="submit"]`, `mat-toolbar[color="primary"] h2`.

### Common Material Selector Patterns

```
# Find a form field by its control name
input[formControlName="name"]
mat-select[formControlName="type"]
mat-slide-toggle[formControlName="gateway"]
textarea[formControlName="description"]

# Find table column content
[matColumnDef="name"] mat-cell

# Find buttons by tooltip
button[matTooltip="Search"]

# Find a specific tab
mat-tab-group mat-tab (by index or label text)

# Find menu items (after opening menu)
button[mat-menu-item]

# Find dialog actions
[mat-dialog-actions] button[mat-raised-button]
```

---

## 6. Form Structure Guide

All entity detail forms extend the abstract `EntityComponent<T>` directive (`@home/components/entity/entity.component.ts`).

**Key properties:**
- `entityForm: UntypedFormGroup` — the reactive form instance
- `entity: T` — current entity data (input)
- `isEdit: boolean` — whether form is in edit mode (input)

**Lifecycle:**

1. **`buildForm(entity: T): UntypedFormGroup`** — Called in constructor. Creates the FormGroup with controls, validators, and initial values. Subscribe to valueChanges here for cascading updates.

2. **`updateForm(entity: T): void`** — Called when entity input changes. Uses `entityForm.patchValue({...}, {emitEvent: false})` to populate form without triggering change events.

3. **`updateFormState(): void`** — Called when `isEdit` changes. Calls `entityForm.enable()` or `entityForm.disable()` with `{emitEvent: false}`.

4. **`entityFormValue(): any`** — Returns `entityForm.getRawValue()` (reads disabled fields too), passed through `prepareFormValue()` (default: `deepTrim`).

**Read-only pattern:** Templates use `<fieldset [disabled]="isEntityReadonly">` to visually disable all inputs. The form group is also programmatically disabled via `entityForm.disable()`.

**Saving:** Merges `{...this.entity, ...this.entityFormValue()}` and calls the HTTP service.

**Common validators:** `Validators.required`, `Validators.maxLength(255)`, custom `validateEmail`.

**Nested FormGroups** — `additionalInfo` is almost always a nested group:
```typescript
additionalInfo: this.fb.group({
  description: [''],
  gateway: [false],
  // ...
})
```

---

## 7. HTTP Services Reference

All services are in `src/app/core/http/`.

| Entity / Area | Service File | Class |
|---------------|-------------|-------|
| Device | `device.service.ts` | `DeviceService` |
| Asset | `asset.service.ts` | `AssetService` |
| Device Profile | `device-profile.service.ts` | `DeviceProfileService` |
| Asset Profile | `asset-profile.service.ts` | `AssetProfileService` |
| Entity View | `entity-view.service.ts` | `EntityViewService` |
| Customer | `customer.service.ts` | `CustomerService` |
| User | `user.service.ts` | `UserService` |
| Tenant | `tenant.service.ts` | `TenantService` |
| Tenant Profile | `tenant-profile.service.ts` | `TenantProfileService` |
| Dashboard | `dashboard.service.ts` | `DashboardService` |
| Rule Chain | `rule-chain.service.ts` | `RuleChainService` |
| Edge | `edge.service.ts` | `EdgeService` |
| Alarm | `alarm.service.ts` | `AlarmService` |
| Alarm Comment | `alarm-comment.service.ts` | `AlarmCommentService` |
| OTA Package | `ota-package.service.ts` | `OtaPackageService` |
| Calculated Fields | `calculated-fields.service.ts` | `CalculatedFieldsService` |
| Widget | `widget.service.ts` | `WidgetService` |
| Resource | `resource.service.ts` | `ResourceService` |
| Queue | `queue.service.ts` | `QueueService` |
| AI Model | `ai-model.service.ts` | `AiModelService` |
| Notification | `notification.service.ts` | `NotificationService` |
| OAuth2 | `oauth2.service.ts` | `OAuth2Service` |
| Domain | `domain.service.ts` | `DomainService` |
| Mobile App | `mobile-app.service.ts` | `MobileAppService` |
| Attribute | `attribute.service.ts` | `AttributeService` |
| Entity Relation | `entity-relation.service.ts` | `EntityRelationService` |
| Entity (generic) | `entity.service.ts` | `EntityService` |
| Event | `event.service.ts` | `EventService` |
| Audit Log | `audit-log.service.ts` | `AuditLogService` |
| Admin Settings | `admin.service.ts` | `AdminService` |
| Version Control | `entities-version-control.service.ts` | `EntitiesVersionControlService` |
| 2FA | `two-factor-authentication.service.ts` | `TwoFactorAuthenticationService` |
| User Settings | `user-settings.service.ts` | `UserSettingsService` |
| API Key | `api-key.service.ts` | `ApiKeyService` |
| Image | `image.service.ts` | `ImageService` |
| Trendz Settings | `trendz-settings.service.ts` | `TrendzSettingsService` |
| Usage Info | `usage-info.service.ts` | `UsageInfoService` |
| Component Descriptor | `component-descriptor.service.ts` | `ComponentDescriptorService` |
| GitHub | `git-hub.service.ts` | `GitHubService` |
| Mobile Application | `mobile-application.service.ts` | `MobileApplicationService` |
| UI Settings | `ui-settings.service.ts` | `UiSettingsService` |
