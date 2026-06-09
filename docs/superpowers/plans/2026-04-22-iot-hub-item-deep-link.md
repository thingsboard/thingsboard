# IoT Hub item deep link — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship `/iot-hub/{itemId}` (latest published version of an item) and `/iot-hub/version/{itemVersionId}` (specific version snapshot, warning gate if unpublished) deep links that resolve a version, navigate to the type-specific browse page, and open the existing detail dialog.

**Architecture:** A router-reachable `TbIotHubItemResolverComponent` owns resolution: fetch by itemId, (optionally) gate on a blocking warning dialog, then `router.navigate` to `/iot-hub/{typeSegment(type)}` carrying the version in `history.state`. The target type-page consumes the state once and opens the existing `TbIotHubItemDetailDialogComponent` via `IotHubActionsService`, with a new `preview` flag that adds an "Unpublished preview" badge. Zero ThingsBoard backend changes — install flows reuse existing versionId endpoints.

**Tech Stack:** Angular 20, Angular Material dialogs, NgRx (for toast dispatch), RxJS. TypeScript strict mode.

**Note on testing:** `ui-ngx` has no frontend test runner wired up (no `npm test` script, no karma/jest config). Each task is verified by `npm run lint` + `npm run build:prod` passing and, for flow-level tasks, a manual dev-server smoke test documented in the final task.

**Prerequisite (external):** The two new IoT Hub endpoints (`GET /api/items/{itemId}/published` and `GET /api/items/{itemId}/latest`) must be deployed on the target IoT Hub instance for end-to-end testing. Until they land, local verification can be done against a mocked IoT Hub or against the published endpoint only. See the spec (`docs/superpowers/specs/2026-04-22-iot-hub-item-deep-link-design.md`) for the full IoT Hub-side contract.

---

## File structure

**Create:**
- `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-deep-link.utils.ts` — UUID check, type→route-segment mapping, `isPublished` predicate
- `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-item-resolver.component.ts` — route-reachable controller
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.ts` — warning dialog logic
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.html` — warning dialog template
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.scss` — warning dialog styles

**Modify:**
- `ui-ngx/src/app/core/http/iot-hub-api.service.ts` — add `getPublishedVersion`, `getLatestVersion`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts` — add `preview` field on data + component
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html` — preview badge markup
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.scss` — preview badge styles
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-actions.service.ts` — propagate `preview` in `openItemDetail`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts` — declare + export warning dialog
- `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-routing.module.ts` — two new child routes (placed last)
- `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub.module.ts` — declare resolver component
- `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-items-page.component.ts` — `maybeOpenDeepLinkedItem` handoff
- `ui-ngx/src/assets/locale/locale.constant-en_US.json` — 9 new i18n keys under the existing `"iot-hub"` block

---

### Task 1: Add i18n keys

Add all new English strings up front so later tasks can reference them freely. Other locales follow the existing project convention (en_US only for new iot-hub keys; translators backfill later).

**Files:**
- Modify: `ui-ngx/src/assets/locale/locale.constant-en_US.json`

- [ ] **Step 1: Add keys inside the existing `"iot-hub"` block**

Open `ui-ngx/src/assets/locale/locale.constant-en_US.json`, find the `"iot-hub"` block (starts at the line matching `"iot-hub": {`), and append the following nine keys to it (place them immediately before the closing `}` of the `"iot-hub"` object, keeping JSON valid — i.e. add a trailing comma to the previous key):

```json
        "item-detail": "IoT Hub item",
        "item-preview": "IoT Hub item preview",
        "unpublished-warning-title": "Unpublished content",
        "unpublished-warning-text": "This is a preview of unpublished content. It has not been reviewed by IoT Hub. Installing unverified content can introduce security and stability risks — only continue if you trust the creator.",
        "unpublished-warning-confirm": "I understand the risk, continue",
        "unpublished-preview": "Unpublished preview",
        "deep-link-invalid-id": "Invalid IoT Hub item link.",
        "deep-link-not-found": "This IoT Hub item doesn't exist or was removed.",
        "deep-link-fetch-failed": "Couldn't load IoT Hub item. Please try again."
```

- [ ] **Step 2: Validate JSON**

Run:
```bash
cd ui-ngx && node -e "JSON.parse(require('fs').readFileSync('src/assets/locale/locale.constant-en_US.json', 'utf8')); console.log('OK')"
```
Expected output: `OK`.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/assets/locale/locale.constant-en_US.json
git commit -m "feat(iot-hub): add i18n keys for item deep-link and unpublished warning"
```

---

### Task 2: Deep-link utility module

A tiny, pure-TS helper file. No Angular deps. Exported functions are used by the resolver and (for `isPublished`) by the detail dialog.

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-deep-link.utils.ts`

- [ ] **Step 1: Create the file**

Create `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-deep-link.utils.ts` with:

```ts
///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isUUID(s: string | null | undefined): s is string {
  return !!s && UUID_RE.test(s);
}

export function typeSegment(t: ItemType): string | undefined {
  switch (t) {
    case ItemType.WIDGET: return 'widgets';
    case ItemType.DASHBOARD: return 'dashboards';
    case ItemType.SOLUTION_TEMPLATE: return 'solution-templates';
    case ItemType.CALCULATED_FIELD: return 'calculated-fields';
    case ItemType.RULE_CHAIN: return 'rule-chains';
    case ItemType.DEVICE: return 'devices';
    default: return undefined;
  }
}

export function isPublished(v: MpItemVersionView): boolean {
  return !!v.publishedTime && v.publishedTime > 0;
}

export interface DeepLinkOpenItem {
  version: MpItemVersionView;
  preview: boolean;
}
```

- [ ] **Step 2: Verify TypeScript compiles**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -20
```
Expected: build succeeds. If `ng build` is too slow for iteration, use `npx tsc --noEmit -p tsconfig.json 2>&1 | grep iot-hub-deep-link || echo 'no type errors in utils file'`.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-deep-link.utils.ts
git commit -m "feat(iot-hub): add deep-link utility helpers (isUUID, typeSegment, isPublished)"
```

---

### Task 3: IoT Hub API service — two new methods

Add `getPublishedVersion` and `getLatestVersion` to `IotHubApiService`. Both hit IoT Hub cross-origin endpoints (same `baseUrl` as existing `/api/versions/published`).

**Files:**
- Modify: `ui-ngx/src/app/core/http/iot-hub-api.service.ts`

- [ ] **Step 1: Add the two methods**

Open `ui-ngx/src/app/core/http/iot-hub-api.service.ts`. Insert the following two methods immediately after `getVersionInfo` (which ends near line 100):

```ts
  public getPublishedVersion(itemId: string, config?: IotHubRequestConfig): Observable<MpItemVersionView> {
    return this.http.get<MpItemVersionView>(
      `${this.baseUrl}/api/items/${itemId}/published`,
      { params: this.buildParams(config) }
    );
  }

  public getLatestVersion(itemId: string, config?: IotHubRequestConfig): Observable<MpItemVersionView> {
    return this.http.get<MpItemVersionView>(
      `${this.baseUrl}/api/items/${itemId}/latest`,
      { params: this.buildParams(config) }
    );
  }
```

Use the exact `Edit` tool call: `old_string` should be the line `  public getVersionReadme(versionId: string, config?: IotHubRequestConfig): Observable<string> {` (and enough context around it), and `new_string` should prepend the two new methods above it.

- [ ] **Step 2: Verify build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -15
```
Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/core/http/iot-hub-api.service.ts
git commit -m "feat(iot-hub): add getPublishedVersion and getLatestVersion API methods"
```

---

### Task 4: Unpublished warning dialog component

New dialog styled after `TbIotHubDeleteDialogComponent`: title, description, item summary, Cancel + danger-accented confirm button. Returns `boolean` from `afterClosed`.

**Files:**
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.ts`
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.html`
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.scss`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts`

- [ ] **Step 1: Create the component .ts**

Create `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.ts`:

```ts
///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

export interface IotHubUnpublishedWarningDialogData {
  item: MpItemVersionView;
}

@Component({
  selector: 'tb-iot-hub-unpublished-warning-dialog',
  standalone: false,
  templateUrl: './iot-hub-unpublished-warning-dialog.component.html',
  styleUrls: ['./iot-hub-unpublished-warning-dialog.component.scss']
})
export class TbIotHubUnpublishedWarningDialogComponent extends DialogComponent<TbIotHubUnpublishedWarningDialogComponent, boolean> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubUnpublishedWarningDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubUnpublishedWarningDialogData
  ) {
    super(store, router, dialogRef);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
```

- [ ] **Step 2: Create the component .html**

Create `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.html`:

```html
<!--

    Copyright © 2016-2026 The Thingsboard Authors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<div class="tb-iot-hub-warning-content">
  <div class="tb-iot-hub-warning-title-row">
    <mat-icon class="tb-iot-hub-warning-icon">warning</mat-icon>
    <h2 class="tb-iot-hub-warning-title">{{ 'iot-hub.unpublished-warning-title' | translate }}</h2>
  </div>
  <p class="tb-iot-hub-warning-text">{{ 'iot-hub.unpublished-warning-text' | translate }}</p>
  <div class="tb-iot-hub-warning-item">
    <span class="tb-iot-hub-warning-item-name">{{ data.item.name }}</span>
    <span class="tb-iot-hub-warning-item-version">v {{ data.item.version }}</span>
  </div>
</div>
<div class="tb-iot-hub-warning-actions">
  <button mat-button (click)="cancel()">{{ 'action.cancel' | translate }}</button>
  <button mat-flat-button color="warn" (click)="confirm()">{{ 'iot-hub.unpublished-warning-confirm' | translate }}</button>
</div>
```

- [ ] **Step 3: Create the component .scss**

Create `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.scss`:

```scss
/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

.tb-iot-hub-warning-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  max-width: 480px;
}

.tb-iot-hub-warning-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tb-iot-hub-warning-icon {
  color: #d32f2f;
  font-size: 28px;
  width: 28px;
  height: 28px;
}

.tb-iot-hub-warning-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 24px;
  letter-spacing: 0.1px;
  color: rgba(0, 0, 0, 0.87);
  margin: 0;
}

.tb-iot-hub-warning-text {
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  letter-spacing: 0.2px;
  color: rgba(0, 0, 0, 0.75);
  margin: 0;
}

.tb-iot-hub-warning-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(211, 47, 47, 0.08);
  border-left: 3px solid #d32f2f;
  border-radius: 2px;

  .tb-iot-hub-warning-item-name {
    font-weight: 600;
    color: rgba(0, 0, 0, 0.87);
  }

  .tb-iot-hub-warning-item-version {
    font-size: 13px;
    color: rgba(0, 0, 0, 0.54);
  }
}

.tb-iot-hub-warning-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px;
}
```

- [ ] **Step 4: Register in `IotHubComponentsModule`**

Open `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts`.

Add the import line after the existing `TbIotHubDeleteDialogComponent` import:

```ts
import { TbIotHubUnpublishedWarningDialogComponent } from './iot-hub-unpublished-warning-dialog.component';
```

In the `declarations` array, add `TbIotHubUnpublishedWarningDialogComponent` after `TbIotHubDeleteDialogComponent`.

In the `exports` array, add `TbIotHubUnpublishedWarningDialogComponent` after `TbIotHubDeleteDialogComponent`.

- [ ] **Step 5: Verify build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -15
```
Expected: build succeeds.

- [ ] **Step 6: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.ts \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.html \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-unpublished-warning-dialog.component.scss \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts
git commit -m "feat(iot-hub): add unpublished content warning dialog"
```

---

### Task 5: Detail dialog `preview` flag + badge, actions service signature

Add `preview?: boolean` to `IotHubItemDetailDialogData`, store on the component, render an "Unpublished preview" badge in the meta-bar next to the version chip. Extend `IotHubActionsService.openItemDetail` to forward the flag.

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.scss`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-actions.service.ts`

- [ ] **Step 1: Extend `IotHubItemDetailDialogData` + component field**

In `iot-hub-item-detail-dialog.component.ts`, find the `IotHubItemDetailDialogData` interface (around line 43) and add the optional `preview` field:

```ts
export interface IotHubItemDetailDialogData {
  item: MpItemVersionView;
  installedItem?: IotHubInstalledItem;
  installedItemsCount?: number;
  mode?: IotHubItemDetailDialogMode;
  showCreator?: boolean;
  preview?: boolean;
}
```

In the component class, add a new public field below `showCreator`:

```ts
  preview: boolean;
```

In the constructor body (below `this.showCreator = data.showCreator !== false;`), add:

```ts
    this.preview = data.preview === true;
```

- [ ] **Step 2: Add badge markup in template**

In `iot-hub-item-detail-dialog.component.html`, find the block starting with `<span class="dlg-subtitle-group">` that contains the version icon (around line 39, the `update` mat-icon). Immediately **after** the closing `</span>` of that version group (just before the `@if (item.publishedTime)` block), insert:

```html
        @if (preview) {
          <span class="dlg-dot"></span>
          <span class="dlg-subtitle-group tb-unpublished-preview-badge">
            <mat-icon class="dlg-subtitle-icon">warning</mat-icon>
            <span>{{ 'iot-hub.unpublished-preview' | translate }}</span>
          </span>
        }
```

- [ ] **Step 3: Style the badge**

Append to `iot-hub-item-detail-dialog.component.scss`:

```scss
.dlg-subtitle-group.tb-unpublished-preview-badge {
  color: #d32f2f;
  font-weight: 600;

  .dlg-subtitle-icon {
    color: #d32f2f;
  }
}
```

- [ ] **Step 4: Extend `IotHubActionsService.openItemDetail`**

In `iot-hub-actions.service.ts`, replace the `openItemDetail` method signature and body with:

```ts
  openItemDetail(item: MpItemVersionView, installedItem?: IotHubInstalledItem, installedItemsCount?: number,
                 mode?: IotHubItemDetailDialogMode, showCreator?: boolean, preview?: boolean): Observable<any> {
    return this.dialog.open(TbIotHubItemDetailDialogComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      autoFocus: false,
      data: { item, installedItem, installedItemsCount, mode, showCreator, preview } as IotHubItemDetailDialogData
    }).afterClosed();
  }
```

Existing callers pass 5 or fewer args and remain valid — the new parameter is optional.

- [ ] **Step 5: Verify build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -15
```
Expected: build succeeds.

- [ ] **Step 6: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.scss \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-actions.service.ts
git commit -m "feat(iot-hub): add unpublished preview badge to item detail dialog"
```

---

### Task 6: Resolver component

The heart of the feature. Fetches the version, gates on warning for unpublished preview, redirects to the type-page with router state.

**Files:**
- Create: `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-item-resolver.component.ts`

- [ ] **Step 1: Create the component**

Create `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-item-resolver.component.ts`:

```ts
///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import {
  DeepLinkOpenItem,
  isPublished,
  isUUID,
  typeSegment
} from './iot-hub-deep-link.utils';
import {
  IotHubUnpublishedWarningDialogData,
  TbIotHubUnpublishedWarningDialogComponent
} from '@home/components/iot-hub/iot-hub-unpublished-warning-dialog.component';

@Component({
  selector: 'tb-iot-hub-item-resolver',
  standalone: false,
  template: ''
})
export class TbIotHubItemResolverComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private store: Store<AppState>,
    private translate: TranslateService,
    private iotHubApi: IotHubApiService
  ) {}

  ngOnInit(): void {
    const itemId = this.route.snapshot.paramMap.get('itemId');
    const preview = this.route.snapshot.data['preview'] === true;

    if (!isUUID(itemId)) {
      this.failTo('iot-hub.deep-link-invalid-id');
      return;
    }

    const fetch$ = preview
      ? this.iotHubApi.getLatestVersion(itemId, { ignoreErrors: true })
      : this.iotHubApi.getPublishedVersion(itemId, { ignoreErrors: true });

    fetch$.subscribe({
      next: v => this.handleResolved(v, preview),
      error: err => {
        const key = err?.status === 404
          ? 'iot-hub.deep-link-not-found'
          : 'iot-hub.deep-link-fetch-failed';
        this.failTo(key);
      }
    });
  }

  private handleResolved(version: MpItemVersionView, preview: boolean): void {
    const segment = typeSegment(version.type);
    if (!segment) {
      this.failTo('iot-hub.deep-link-fetch-failed');
      return;
    }

    const showWarning = preview && !isPublished(version);

    if (showWarning) {
      this.dialog.open<
        TbIotHubUnpublishedWarningDialogComponent,
        IotHubUnpublishedWarningDialogData,
        boolean
      >(TbIotHubUnpublishedWarningDialogComponent, {
        panelClass: ['tb-dialog'],
        disableClose: true,
        autoFocus: false,
        data: { item: version }
      }).afterClosed().subscribe(confirmed => {
        if (confirmed) {
          this.openOnTypePage(version, segment, true);
        } else {
          this.router.navigate(['/iot-hub'], { replaceUrl: true });
        }
      });
    } else {
      this.openOnTypePage(version, segment, false);
    }
  }

  private openOnTypePage(version: MpItemVersionView, segment: string, preview: boolean): void {
    const openItem: DeepLinkOpenItem = { version, preview };
    this.router.navigate(['/iot-hub', segment], {
      state: { openItem },
      replaceUrl: true
    });
  }

  private failTo(messageKey: string): void {
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant(messageKey),
      type: 'error',
      duration: 5000
    }));
    this.router.navigate(['/iot-hub'], { replaceUrl: true });
  }
}
```

- [ ] **Step 2: Verify TypeScript compiles (the module wiring comes in Task 7)**

The component is not yet declared in any module, so a full `ng build` will fail. Run a type-check only on this file:

```bash
cd ui-ngx && npx tsc --noEmit -p tsconfig.json 2>&1 | grep -E "iot-hub-item-resolver|iot-hub-deep-link" || echo "no type errors in new files"
```
Expected: `no type errors in new files`.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-item-resolver.component.ts
git commit -m "feat(iot-hub): add item resolver component for deep links"
```

---

### Task 7: Routing + module registration

Register the resolver component in `IotHubModule` and add the two new child routes to `iot-hub-routing.module.ts`. Reserved words must be matched first, so the `:itemId` wildcard routes go **last** in the children array.

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub.module.ts`
- Modify: `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-routing.module.ts`

- [ ] **Step 1: Declare resolver in `IotHubModule`**

In `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub.module.ts`, add the import:

```ts
import { TbIotHubItemResolverComponent } from './iot-hub-item-resolver.component';
```

In the `declarations` array, add `TbIotHubItemResolverComponent` after `TbIotHubSearchPageComponent`.

- [ ] **Step 2: Add routes**

In `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-routing.module.ts`, add the import:

```ts
import { TbIotHubItemResolverComponent } from './iot-hub-item-resolver.component';
```

In the `children` array of the `/iot-hub` route, **immediately before** the closing `]` (after the existing `creator/:creatorId` entry), insert:

```ts
      {
        path: ':itemId',
        component: TbIotHubItemResolverComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.item-detail'
        }
      },
      {
        path: ':itemId/preview',
        component: TbIotHubItemResolverComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'iot-hub.item-preview',
          preview: true
        }
      }
```

Make sure there is a comma after the preceding `creator/:creatorId` entry.

- [ ] **Step 3: Verify build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -15
```
Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub.module.ts \
        ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-routing.module.ts
git commit -m "feat(iot-hub): wire resolver component into routing and module"
```

---

### Task 8: Type-page handoff

`TbIotHubItemsPageComponent` consumes `history.state.openItem` on init: resolves installed state, opens the detail dialog with the `preview` flag, then clears the state entry so refresh does not re-open.

**Files:**
- Modify: `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-items-page.component.ts`

- [ ] **Step 1: Extend imports + constructor**

Open `ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-items-page.component.ts`.

Replace the import block (top of file, currently lines 17-20) with:

```ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { PageLink } from '@shared/models/page/page-link';
import { DeepLinkOpenItem } from './iot-hub-deep-link.utils';
```

Replace the constructor with:

```ts
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService
  ) {}
```

- [ ] **Step 2: Extend `ngOnInit` and add handoff method**

Replace the existing `ngOnInit` body with:

```ts
  ngOnInit(): void {
    const itemType = this.route.snapshot.data['itemType'] as string;
    this.config = PAGE_CONFIGS[itemType];
    this.loadInstalledCount();
    this.maybeOpenDeepLinkedItem();
  }
```

Add these two new methods anywhere in the class (e.g. after `loadInstalledCount`):

```ts
  private maybeOpenDeepLinkedItem(): void {
    const openItem = history.state?.openItem as DeepLinkOpenItem | undefined;
    if (!openItem || openItem.version.type !== this.config.type) {
      return;
    }
    history.replaceState({ ...history.state, openItem: undefined }, '');
    this.resolveInstalledItem(openItem.version).subscribe(installed => {
      this.iotHubActions.openItemDetail(
        openItem.version,
        installed ?? undefined,
        installed ? 1 : 0,
        'default',
        true,
        openItem.preview
      ).subscribe();
    });
  }

  private resolveInstalledItem(version: MpItemVersionView): Observable<IotHubInstalledItem | null> {
    return this.iotHubApiService
      .getInstalledItems(new PageLink(1), undefined, version.itemId, { ignoreLoading: true })
      .pipe(map(page => page.data[0] ?? null));
  }
```

- [ ] **Step 3: Verify build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -15
```
Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/modules/home/pages/iot-hub/iot-hub-items-page.component.ts
git commit -m "feat(iot-hub): open deep-linked item from history state on type-page init"
```

---

### Task 9: Lint, build, and manual smoke test

Final verification gate. Runs the lint and production build, then walks each user-facing flow in a dev server to confirm behavior.

**Files:** none (verification only)

- [ ] **Step 1: Lint**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng lint 2>&1 | tail -30
```
Expected: no new errors introduced by files created/modified in tasks 1–8. Warnings in unrelated files are acceptable.

- [ ] **Step 2: Production build**

Run:
```bash
cd ui-ngx && node --max_old_space_size=4096 ./node_modules/@angular/cli/bin/ng build --configuration production 2>&1 | tail -20
```
Expected: build completes without errors.

- [ ] **Step 3: Start dev server**

Run:
```bash
cd ui-ngx && node --max_old_space_size=8048 ./node_modules/@angular/cli/bin/ng serve --configuration development --host 0.0.0.0
```
Dev server should be reachable at `http://localhost:4200`. Log in as a TENANT_ADMIN user. (A running ThingsBoard backend at the default proxy target is required per `proxy.conf.js`.)

- [ ] **Step 4: Manual smoke test — published deep link**

Pick an itemId from `/iot-hub/widgets` (the card's click handler logs it, or inspect the dialog URL via browser dev tools; alternatively, query `iotHubBaseUrl/api/versions/published` and copy an `itemId`).

Navigate to `http://localhost:4200/iot-hub/{that-itemId}`. Verify:
- URL bar ends at `/iot-hub/widgets` (or the correct type page for the item) after resolution
- Detail dialog opens with the item's info, no "Unpublished preview" badge, and Install/Update actions behave normally
- Closing the dialog leaves the user on the type-page

Navigate to `http://localhost:4200/iot-hub/00000000-0000-0000-0000-000000000000` (a valid-shaped but non-existent UUID). Verify:
- URL bar ends at `/iot-hub`
- Red toast: "This IoT Hub item doesn't exist or was removed."

Navigate to `http://localhost:4200/iot-hub/not-a-uuid`. Verify:
- URL bar ends at `/iot-hub`
- Red toast: "Invalid IoT Hub item link."

- [ ] **Step 5: Manual smoke test — preview deep link (requires IoT Hub `/latest` endpoint)**

**If the IoT Hub-side endpoints are not yet deployed**, skip this step and record that end-to-end preview verification is deferred until the IoT Hub PR lands. The per-URL flow is exercised in Step 4; the preview URL hitting a non-existent endpoint will trigger the "fetch failed" toast, which is the correct fallback behavior.

**If the endpoints are available**, navigate to `http://localhost:4200/iot-hub/{unpublished-itemId}/preview`. Verify:
- Warning dialog opens immediately with the item's name/version, red warning icon, and two buttons
- "Cancel" → closes dialog, lands on `/iot-hub` (home)
- Revisit the URL, click "I understand the risk, continue" → URL advances to `/iot-hub/{typePage}`, detail dialog opens with the red "Unpublished preview" badge in the meta bar
- Install button still works end-to-end (calls the TB install endpoint, which proxies to IoT Hub using the unpublished versionId)

Navigate to `http://localhost:4200/iot-hub/{published-only-itemId}/preview`. Verify:
- No warning shown (preview fell back to the published version)
- Detail dialog opens with **no** preview badge
- Behavior matches the regular published URL

- [ ] **Step 6: Commit the final verification (if any lint/build fixes were needed)**

If the smoke test uncovered issues that required fixes, stage and commit them with a message like `fix(iot-hub): address smoke-test findings for deep-link flow`. Otherwise, this step is a no-op — all previous commits already capture the work.

---

## IoT Hub-side changes required (recap)

One new endpoint plus behavior + CORS contracts on the existing by-versionId family:

1. **New endpoint** `GET /api/items/{itemId}/published` — latest PUBLISHED version as `MpItemVersionView`; `404` if none. Anonymous cross-origin. Powers the `/iot-hub/{itemId}` URL.
2. **`GET /api/versions/{versionId}`** must return the requested version regardless of state (PUBLISHED / DRAFT / PENDING_REVIEW / …). Anonymous cross-origin; versionId UUID is the soft-secret gate. Powers the `/iot-hub/version/{itemVersionId}` URL.
3. **`MpItemVersionView`** must allow the frontend to tell published from unpublished. Either `publishedTime` must be falsy (`0`/`null`) for non-published versions, or add an explicit `state` field. Frontend's `isPublished()` uses `publishedTime > 0` today.
4. **Related by-versionId endpoints** must also serve unpublished versions (required for install-from-deep-link):
   - `GET /api/versions/{versionId}/readme`
   - `GET /api/versions/{versionId}/fileData`
   - `POST /api/versions/{versionId}/install`
5. **Install counter policy** for unpublished versions — recommended: skip counting.
6. **CORS** on `/api/items/{itemId}/published` and the `/api/versions/{versionId}/...` family must permit cross-origin GET from any origin.

These live in the IoT Hub repository, not ThingsBoard CE.
