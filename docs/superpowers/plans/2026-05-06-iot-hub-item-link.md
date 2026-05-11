# IoT Hub `${item-link:uuid}` Markdown Component — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `${item-link:<uuid>}` markdown placeholder that renders an IoT Hub marketplace item card (thumbnail + name + creator) inline in readmes and install instructions, opening the linked item in a new tab.

**Architecture:** A pure-string utility rewrites `${item-link:<uuid>}` to a `<tb-iot-hub-item-link-card itemId="...">` Angular component tag before the markdown reaches `tb-markdown`. The component is declared in a small `IotHubItemLinkModule` that each `tb-markdown` instance receives as `additionalCompileModules`. The component owns its own fetch (via `IotHubApiService.getPublishedVersion`), state (`loading` / `loaded` / `unavailable`), and click target (`/iot-hub/{itemId}` with `target="_blank"`).

**Tech Stack:** Angular 20 (TypeScript, Material), `tb-markdown` (NgModule-based dynamic compilation), `IotHubApiService` (existing), Tailwind for utility classes, component-scoped SCSS.

**Spec:** `docs/superpowers/specs/2026-05-06-iot-hub-item-link.md`

**File structure:**

New files:
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-markdown.utils.ts` — `replaceItemLinkPlaceholders` + regex
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.ts`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.html`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.scss`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link.module.ts`

Modified files:
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts` — import `IotHubItemLinkModule`
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts` — call utility in `loadReadme()`, expose compile modules
- `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html` — bind `[additionalCompileModules]` on readme `<tb-markdown>`
- `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts` — add `^item-link:(uuid)$` branch in `resolveVariables()`, expose compile modules
- `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html` — bind `[additionalCompileModules]`
- `ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.ts` — pre-process `details`, expose compile modules
- `ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.html` — bind `[additionalCompileModules]`
- `ui-ngx/src/assets/locale/locale.constant-en_US.json` — add `iot-hub.item-link-unavailable`

---

## Task 1: Create placeholder utility

**Files:**
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-markdown.utils.ts`

- [ ] **Step 1: Create the utility file**

Create `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-markdown.utils.ts`:

```typescript
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

export const ITEM_LINK_PLACEHOLDER_REGEX =
  /\$\{item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\}/g;

export const ITEM_LINK_KEY_REGEX =
  /^item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$/;

export function itemLinkCardTag(itemId: string): string {
  return `<tb-iot-hub-item-link-card itemId="${itemId}"></tb-iot-hub-item-link-card>`;
}

export function replaceItemLinkPlaceholders(markdown: string): string {
  if (!markdown) {
    return markdown;
  }
  return markdown.replace(ITEM_LINK_PLACEHOLDER_REGEX, (_match, uuid) => itemLinkCardTag(uuid));
}
```

- [ ] **Step 2: Sanity-check the regex**

Write a temporary script `/tmp/check-item-link-regex.js` with this exact content:

```javascript
const re = /\$\{item-link:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\}/g;
const cases = [
  ['${item-link:11111111-2222-3333-4444-555555555555}', 'valid'],
  ['${item-link:not-a-uuid}',                          'invalid'],
  ['Two: ${item-link:11111111-2222-3333-4444-555555555555} and ${item-link:aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee}', 'two valid'],
  ['```\n${item-link:11111111-2222-3333-4444-555555555555}\n```',  'inside code fence (still replaced — documented behavior)']
];
for (const [input, label] of cases) {
  console.log(label + ':', input.replace(re, (_m, u) => '<TAG itemId=' + u + '>'));
}
```

Run it:
```bash
node /tmp/check-item-link-regex.js
```

Expected output:
```
valid: <TAG itemId=11111111-2222-3333-4444-555555555555>
invalid: ${item-link:not-a-uuid}
two valid: Two: <TAG itemId=11111111-2222-3333-4444-555555555555> and <TAG itemId=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee>
inside code fence (still replaced — documented behavior): ```
<TAG itemId=11111111-2222-3333-4444-555555555555>
```
```

Then delete the script: `rm /tmp/check-item-link-regex.js`.

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-markdown.utils.ts
git commit -m "feat(iot-hub): add item-link placeholder utility for markdown"
```

---

## Task 2: Create item-link card component (skeleton + loaded states, image-thumbnail items)

**Files:**
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.ts`
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.html`
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link-card.component.scss`

- [ ] **Step 1: Create the directory**

```bash
mkdir -p ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card
```

- [ ] **Step 2: Create `iot-hub-item-link-card.component.ts`**

```typescript
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

import { Component, Input, OnInit } from '@angular/core';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';

type CardState = 'loading' | 'loaded' | 'unavailable';

const COMPACT_TYPES: ReadonlySet<ItemType> = new Set([
  ItemType.CALCULATED_FIELD,
  ItemType.ALARM_RULE,
  ItemType.RULE_CHAIN
]);

@Component({
  selector: 'tb-iot-hub-item-link-card',
  standalone: false,
  templateUrl: './iot-hub-item-link-card.component.html',
  styleUrls: ['./iot-hub-item-link-card.component.scss']
})
export class TbIotHubItemLinkCardComponent implements OnInit {

  @Input() itemId!: string;

  state: CardState = 'loading';
  item: MpItemVersionView | null = null;

  constructor(private iotHubApiService: IotHubApiService) {}

  ngOnInit(): void {
    if (!this.itemId) {
      this.state = 'unavailable';
      return;
    }
    this.iotHubApiService
      .getPublishedVersion(this.itemId, { ignoreErrors: true, ignoreLoading: true })
      .subscribe({
        next: item => {
          this.item = item;
          this.state = item ? 'loaded' : 'unavailable';
        },
        error: () => {
          this.state = 'unavailable';
        }
      });
  }

  isCompact(): boolean {
    return !!this.item && COMPACT_TYPES.has(this.item.type);
  }

  getImageUrl(): string | null {
    return this.item?.image
      ? this.iotHubApiService.resolveResourceUrl(this.item.image)
      : null;
  }

  getCompactIcon(): string {
    if (!this.item) {
      return 'category';
    }
    if (this.item.icon) {
      return this.item.icon;
    }
    switch (this.item.type) {
      case ItemType.CALCULATED_FIELD: return 'functions';
      case ItemType.ALARM_RULE: return 'notification_important';
      case ItemType.RULE_CHAIN: return 'account_tree';
      default: return 'category';
    }
  }

  getTypeIcon(): string {
    if (!this.item) {
      return 'category';
    }
    switch (this.item.type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboard';
      case ItemType.SOLUTION_TEMPLATE: return 'integration_instructions';
      case ItemType.CALCULATED_FIELD: return 'functions';
      case ItemType.ALARM_RULE: return 'notification_important';
      case ItemType.RULE_CHAIN: return 'account_tree';
      case ItemType.DEVICE: return 'memory';
      default: return 'category';
    }
  }

  getCompactColor(): string {
    return this.item?.color || '#048ad3';
  }

  getHref(): string {
    return `/iot-hub/${this.itemId}`;
  }
}
```

- [ ] **Step 3: Create `iot-hub-item-link-card.component.html`**

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
@switch (state) {
  @case ('loading') {
    <div class="tb-iot-hub-item-link-card tb-iot-hub-item-link-card--skeleton" aria-busy="true">
      <div class="tb-iot-hub-item-link-thumb tb-iot-hub-item-link-skeleton-block"></div>
      <div class="tb-iot-hub-item-link-text">
        <span class="tb-iot-hub-item-link-skeleton-line"></span>
        <span class="tb-iot-hub-item-link-skeleton-line tb-iot-hub-item-link-skeleton-line--short"></span>
      </div>
    </div>
  }
  @case ('loaded') {
    <a [href]="getHref()"
       target="_blank"
       rel="noopener noreferrer"
       class="tb-iot-hub-item-link-card">
      @if (isCompact()) {
        <div class="tb-iot-hub-item-link-thumb tb-iot-hub-item-link-thumb--compact"
             [style.background]="getCompactColor()">
          <tb-icon class="tb-iot-hub-item-link-thumb-icon">{{ getCompactIcon() }}</tb-icon>
        </div>
      } @else {
        <div class="tb-iot-hub-item-link-thumb">
          @if (getImageUrl(); as imgUrl) {
            <img [src]="imgUrl" alt="">
          } @else {
            <mat-icon class="tb-iot-hub-item-link-thumb-fallback">{{ getTypeIcon() }}</mat-icon>
          }
        </div>
      }
      <div class="tb-iot-hub-item-link-text">
        <span class="tb-iot-hub-item-link-name">{{ item?.name }}</span>
        <span class="tb-iot-hub-item-link-author">
          <mat-icon>person</mat-icon>
          {{ item?.creatorDisplayName }}
        </span>
      </div>
    </a>
  }
  @case ('unavailable') {
    <div class="tb-iot-hub-item-link-card tb-iot-hub-item-link-card--unavailable">
      <div class="tb-iot-hub-item-link-thumb tb-iot-hub-item-link-thumb--unavailable">
        <mat-icon>link_off</mat-icon>
      </div>
      <div class="tb-iot-hub-item-link-text">
        <span class="tb-iot-hub-item-link-name">{{ 'iot-hub.item-link-unavailable' | translate }}</span>
      </div>
    </div>
  }
}
```

- [ ] **Step 4: Create `iot-hub-item-link-card.component.scss`**

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

:host {
  display: block;
  margin: 12px 0;
}

.tb-iot-hub-item-link-card {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  width: 320px;
  max-width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: #fff;
  text-decoration: none;
  color: inherit;
  transition: background-color 120ms ease, border-color 120ms ease, box-shadow 120ms ease;

  &:hover:not(.tb-iot-hub-item-link-card--unavailable):not(.tb-iot-hub-item-link-card--skeleton) {
    background: rgba(4, 138, 211, 0.04);
    border-color: rgba(4, 138, 211, 0.32);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }

  &--unavailable {
    opacity: 0.6;
    cursor: default;
  }

  &--skeleton {
    cursor: default;
  }
}

.tb-iot-hub-item-link-thumb {
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #f4f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &--compact {
    color: #fff;
  }

  &--unavailable {
    color: rgba(0, 0, 0, 0.45);
  }
}

.tb-iot-hub-item-link-thumb-icon {
  font-size: 26px;
  width: 26px;
  height: 26px;
  line-height: 26px;
  color: #fff;
}

.tb-iot-hub-item-link-thumb-fallback {
  font-size: 26px;
  width: 26px;
  height: 26px;
  color: rgba(0, 0, 0, 0.45);
}

.tb-iot-hub-item-link-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.tb-iot-hub-item-link-name {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tb-iot-hub-item-link-author {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
  line-height: 1.3;

  mat-icon {
    font-size: 14px;
    width: 14px;
    height: 14px;
  }
}

.tb-iot-hub-item-link-skeleton-block,
.tb-iot-hub-item-link-skeleton-line {
  background: linear-gradient(
    90deg,
    rgba(0, 0, 0, 0.06) 0%,
    rgba(0, 0, 0, 0.12) 50%,
    rgba(0, 0, 0, 0.06) 100%
  );
  background-size: 200% 100%;
  animation: tb-iot-hub-item-link-shimmer 1.4s ease-in-out infinite;
  border-radius: 4px;
}

.tb-iot-hub-item-link-skeleton-line {
  display: block;
  height: 12px;
  width: 180px;
  margin: 4px 0;

  &--short {
    width: 96px;
  }
}

@keyframes tb-iot-hub-item-link-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

- [ ] **Step 5: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card
git commit -m "feat(iot-hub): add TbIotHubItemLinkCardComponent for markdown item links"
```

---

## Task 3: Create the wrapper module and register it

**Files:**
- Create: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link.module.ts`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts`

- [ ] **Step 1: Create `iot-hub-item-link.module.ts`**

```typescript
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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { TbIotHubItemLinkCardComponent } from './iot-hub-item-link-card.component';

@NgModule({
  declarations: [TbIotHubItemLinkCardComponent],
  imports: [CommonModule, SharedModule],
  exports: [TbIotHubItemLinkCardComponent]
})
export class IotHubItemLinkModule {}
```

- [ ] **Step 2: Register module in `IotHubComponentsModule`**

Modify `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts` — add the import and add `IotHubItemLinkModule` to the `imports` array.

After the existing imports near line 32, add:

```typescript
import { IotHubItemLinkModule } from './iot-hub-item-link-card/iot-hub-item-link.module';
```

Then update the `imports` array of the `@NgModule` decorator from:

```typescript
  imports: [
    CommonModule,
    SharedModule
  ],
```

to:

```typescript
  imports: [
    CommonModule,
    SharedModule,
    IotHubItemLinkModule
  ],
```

- [ ] **Step 3: Build to verify the module wires up**

```bash
cd ui-ngx && npx ng build --configuration=development 2>&1 | tail -40
```

Expected: build succeeds (warnings allowed; no errors mentioning `TbIotHubItemLinkCardComponent` or `IotHubItemLinkModule`).

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link.module.ts \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-components.module.ts
git commit -m "feat(iot-hub): wrap item-link card in IotHubItemLinkModule"
```

---

## Task 4: Add translation key

**Files:**
- Modify: `ui-ngx/src/assets/locale/locale.constant-en_US.json`

- [ ] **Step 1: Add the translation key**

Open `ui-ngx/src/assets/locale/locale.constant-en_US.json`. Locate the `"iot-hub": { ... }` block that begins around line 3701 (the one that opens with `"iot-hub": "IoT Hub",`). Add a new entry next to similar one-liner keys (e.g., right after `"installed-from-iot-hub": "Installed from IoT Hub",` near line 3719):

```json
        "item-link-unavailable": "Item unavailable",
```

Make sure the surrounding commas are correct — the new line ends with a comma, and the line above also ends with a comma.

- [ ] **Step 2: Verify JSON is valid**

```bash
node -e "JSON.parse(require('fs').readFileSync('ui-ngx/src/assets/locale/locale.constant-en_US.json', 'utf8')); console.log('OK');"
```
Expected output: `OK`

- [ ] **Step 3: Commit**

```bash
git add ui-ngx/src/assets/locale/locale.constant-en_US.json
git commit -m "feat(iot-hub): add item-link-unavailable translation key"
```

---

## Task 5: Wire into item detail dialog (readme)

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html`

- [ ] **Step 1: Update the .ts file to pre-process readme and expose compile modules**

In `iot-hub-item-detail-dialog.component.ts`:

Add to the imports near the existing `resolveDocLinkPlaceholders` import:

```typescript
import { replaceItemLinkPlaceholders } from './iot-hub-markdown.utils';
import { IotHubItemLinkModule } from './iot-hub-item-link-card/iot-hub-item-link.module';
import { Type } from '@angular/core';
```

(`Type` may already be imported via Angular core; merge into the existing `@angular/core` import if so.)

Inside the class, near the other readonly fields (after `readonly ItemType = ItemType;`), add:

```typescript
  readonly itemLinkCompileModules: Type<any>[] = [IotHubItemLinkModule];
```

Update the existing `loadReadme()` method body (currently at line 307-312):

```typescript
  private loadReadme(): void {
    const versionId = this.item.id as string;
    this.iotHubApiService.getVersionReadme(versionId, { ignoreLoading: true }).subscribe(
      content => this.readmeContent = replaceItemLinkPlaceholders(
        this.resolveDocLinks(this.prefixResourceUrls(content || ''))
      )
    );
  }
```

- [ ] **Step 2: Update the template to pass compile modules**

In `iot-hub-item-detail-dialog.component.html` at line 302, change:

```html
          <tb-markdown [data]="readmeContent"></tb-markdown>
```

to:

```html
          <tb-markdown [data]="readmeContent"
                       [additionalCompileModules]="itemLinkCompileModules"></tb-markdown>
```

Leave the changelog `<tb-markdown>` (line 306) unchanged — changelog is out of scope per the spec.

- [ ] **Step 3: Build**

```bash
cd ui-ngx && npx ng build --configuration=development 2>&1 | tail -20
```

Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.ts \
        ui-ngx/src/app/modules/home/components/iot-hub/iot-hub-item-detail-dialog.component.html
git commit -m "feat(iot-hub): render \${item-link} cards in item readme"
```

---

## Task 6: Wire into device install dialog (instructions)

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts`
- Modify: `ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html`

- [ ] **Step 1: Add imports and compile-modules field**

In `device-install-dialog.component.ts`, add to the imports:

```typescript
import { IotHubItemLinkModule } from '../iot-hub-item-link-card/iot-hub-item-link.module';
import { ITEM_LINK_KEY_REGEX, itemLinkCardTag } from '../iot-hub-markdown.utils';
import { Type } from '@angular/core';
```

(Merge `Type` into the existing `@angular/core` import line.)

Inside the class, alongside other readonly fields, add:

```typescript
  readonly itemLinkCompileModules: Type<any>[] = [IotHubItemLinkModule];
```

- [ ] **Step 2: Add `^item-link:(uuid)$` branch inside `resolveVariables`**

In `resolveVariables()` (currently at line 427-477 in the same file), add a new clause **immediately after** the `gateway.downloadButton` clause (around line 444-446) and before the callout matcher.

Change this section:

```typescript
      // Special action placeholders
      if (key === 'gateway.downloadButton') {
        return '<a href="#" data-action="download-gateway-docker-compose" class="tb-download-btn">⬇ Download docker-compose.yml</a>';
      }
      // Callout boxes: ${note(...)}, ${warn(...)}, ${error(...)}
```

to:

```typescript
      // Special action placeholders
      if (key === 'gateway.downloadButton') {
        return '<a href="#" data-action="download-gateway-docker-compose" class="tb-download-btn">⬇ Download docker-compose.yml</a>';
      }
      // IoT Hub item link card: ${item-link:<uuid>}
      const itemLinkMatch = key.match(ITEM_LINK_KEY_REGEX);
      if (itemLinkMatch) {
        return itemLinkCardTag(itemLinkMatch[1]);
      }
      // Callout boxes: ${note(...)}, ${warn(...)}, ${error(...)}
```

- [ ] **Step 3: Update the template**

In `device-install-dialog.component.html` at line 26-28, change:

```html
          <tb-markdown [data]="ws.markdown"
                       (ready)="onMarkdownReady(instructionContainer)">
          </tb-markdown>
```

to:

```html
          <tb-markdown [data]="ws.markdown"
                       [additionalCompileModules]="itemLinkCompileModules"
                       (ready)="onMarkdownReady(instructionContainer)">
          </tb-markdown>
```

- [ ] **Step 4: Build**

```bash
cd ui-ngx && npx ng build --configuration=development 2>&1 | tail -20
```

Expected: build succeeds.

- [ ] **Step 5: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.ts \
        ui-ngx/src/app/modules/home/components/iot-hub/device-install-dialog/device-install-dialog.component.html
git commit -m "feat(iot-hub): render \${item-link} cards in device install instructions"
```

---

## Task 7: Wire into solution install dialog (instructions)

**Files:**
- Modify: `ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.ts`
- Modify: `ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.html`

- [ ] **Step 1: Update the .ts file**

Replace the contents of `ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.ts` with:

```typescript
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

import { Component, Inject, Type } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import {
  IotHubItemLinkModule
} from '@home/components/iot-hub/iot-hub-item-link-card/iot-hub-item-link.module';
import {
  replaceItemLinkPlaceholders
} from '@home/components/iot-hub/iot-hub-markdown.utils';

export interface SolutionInstallDialogData {
  descriptor: SolutionTemplateInstalledItemDescriptor;
  instructions?: boolean;
}

@Component({
  selector: 'tb-solution-install-dialog',
  templateUrl: './solution-install-dialog.component.html',
  styleUrls: ['./solution-install-dialog.component.scss'],
  standalone: false
})
export class SolutionInstallDialogComponent {

  details: string;
  dashboardId: string | null;
  instructions: boolean;

  readonly itemLinkCompileModules: Type<any>[] = [IotHubItemLinkModule];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: SolutionInstallDialogData,
    private dialogRef: MatDialogRef<SolutionInstallDialogComponent>,
    private router: Router
  ) {
    this.details = replaceItemLinkPlaceholders(data.descriptor.details || '');
    this.dashboardId = data.descriptor.dashboardId?.id || null;
    this.instructions = !!data.instructions;
  }

  gotoMainDashboard(): void {
    if (this.dashboardId) {
      this.dialogRef.close();
      this.router.navigateByUrl(`/dashboards/${this.dashboardId}`);
    }
  }

  close(): void {
    this.dialogRef.close();
  }
}
```

- [ ] **Step 2: Update the template**

In `solution-install-dialog.component.html` at line 29, change:

```html
    <tb-markdown [data]="details" lineNumbers fallbackToPlainMarkdown></tb-markdown>
```

to:

```html
    <tb-markdown [data]="details"
                 [additionalCompileModules]="itemLinkCompileModules"
                 lineNumbers
                 fallbackToPlainMarkdown></tb-markdown>
```

- [ ] **Step 3: Build**

```bash
cd ui-ngx && npx ng build --configuration=development 2>&1 | tail -20
```

Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.ts \
        ui-ngx/src/app/modules/home/components/solution/solution-install-dialog.component.html
git commit -m "feat(iot-hub): render \${item-link} cards in solution install instructions"
```

---

## Task 8: Manual visual QA

**No file changes — verify the feature end-to-end in a browser.**

- [ ] **Step 1: Start the dev server**

```bash
cd ui-ngx && npm start
```

Wait for `Compiled successfully` and `Application bundle generation complete`.

Open `http://localhost:4200` in a browser and log in as a tenant administrator.

- [ ] **Step 2: Verify item readme rendering**

In a separate terminal, query the configured IoT Hub for an item ID you can use as a test target:

```bash
# Use the same baseUrl that your TB instance uses (typically https://iot-hub.thingsboard.io).
# Pick any published item you can find via the marketplace UI's network requests in DevTools,
# e.g. by opening the IoT Hub home page and copying an itemId from a response.
echo "Pick a known itemId from network responses in the IoT Hub UI"
```

Then, for the TEST PASS:
1. Open IoT Hub in the running app, find an item with a non-empty readme (e.g., a Solution Template), open the detail dialog.
2. In DevTools, intercept the readme response by editing it (Network → block + replay, or temporarily edit `readmeContent` via the Angular DevTools), and inject `${item-link:<knownItemId>}` somewhere in the readme markdown.
   - Easier alternative: temporarily hardcode a test placeholder in `loadReadme()` by appending `'\n\n${item-link:<knownItemId>}\n'` to the fetched content, just for this QA pass. Revert before committing.
3. Reload the dialog. Expected:
   - Skeleton card appears briefly with shimmer.
   - Card resolves to: 48-px square thumbnail (image or colored compact icon), item name, person icon + creator name.
   - Hover shows light blue tint and subtle shadow.
   - Click opens `/iot-hub/<itemId>` in a **new tab**, which lands on the item type page with the detail dialog open.
   - Middle-click also opens new tab; ctrl-click does the same.

Revert any hardcoded test data before continuing.

- [ ] **Step 3: Verify unavailable state**

Repeat Step 2 but inject a placeholder with a UUID that does not exist (e.g., `${item-link:00000000-0000-0000-0000-000000000000}`). Expected:
- Card briefly shows skeleton.
- Resolves to disabled card: dim opacity, `link_off` icon in the thumbnail slot, "Item unavailable" label.
- Card is not clickable (no `<a>`, no hover blue).

- [ ] **Step 4: Verify invalid placeholder is left untouched**

Inject `${item-link:not-a-uuid}` into the readme. Expected: the rendered markdown shows the literal text `${item-link:not-a-uuid}` (no card, no error). This proves the regex is strict.

- [ ] **Step 5: Verify device install instructions**

Open any IoT Hub Device item, click Install, and walk to a step whose markdown is dynamically rendered. Inject `${item-link:<knownItemId>}` into one of the markdown templates served by the device package (or temporarily prepend it to `step.markdown` in `device-install-dialog.component.ts` near the existing `resolveVariables` call). Expected: same skeleton → loaded card behavior; click opens `/iot-hub/<itemId>` in a new tab.

Revert temporary edits.

- [ ] **Step 6: Verify solution install instructions**

Install (or reopen the install instructions for) a Solution Template that has a `details` markdown. Inject `${item-link:<knownItemId>}` into `details` (temporarily, in the constructor or via a known solution template whose details you control). Expected: same skeleton → loaded behavior; click opens `/iot-hub/<itemId>` in a new tab.

Revert temporary edits.

- [ ] **Step 7: Confirm no regressions in existing markdown**

In each of the three render sites, verify after the QA edits are reverted:
- Existing `${gateway.downloadButton}` placeholder still renders correctly in the device install instructions (unchanged behavior).
- Existing callout boxes (`${note(...)}`, `${warn(...)}`, `${error(...)}`) still render in the device install instructions.
- Existing `prefixResourceUrls` and `resolveDocLinks` continue to resolve image URLs and doc-link placeholders in readmes.
- Changelog tab in the item detail dialog still renders without an item-link card (it does not bind `additionalCompileModules`).

- [ ] **Step 8: Stop the dev server**

`Ctrl-C` in the terminal running `npm start`.

- [ ] **Step 9: Final commit (if any QA-driven fix-ups were made)**

If QA surfaced any issues that required code changes, commit them with a focused message. Otherwise this step is a no-op.

```bash
git status
# If clean, no commit needed.
```

---

## Self-review checklist (for the implementing agent)

Before declaring complete, verify:

1. **Spec coverage** — every decision in `docs/superpowers/specs/2026-05-06-iot-hub-item-link.md` has a corresponding task above.
2. **No leftover test scaffolding** — temporary hardcoded `${item-link:...}` placeholders used for QA are reverted.
3. **License headers** — every new `.ts` file uses `///` style, every new `.html` uses `<!-- -->`, every new `.scss` uses `/** */`.
4. **Build clean** — `npx ng build --configuration=development` finishes without errors.
5. **Three sites work, fourth doesn't** — readme, device install, solution install all render the card; changelog tab does **not** (by design — out of scope per spec).
