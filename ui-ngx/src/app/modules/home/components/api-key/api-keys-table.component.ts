///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  effect,
  input,
  Renderer2,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';
import { ApiKeysTableConfig } from '@home/components/api-key/api-keys-table-config';
import { ApiKeyService } from '@core/http/api-key.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { UserId } from '@shared/models/id/user-id';

@Component({
  selector: 'tb-api-keys-table',
  templateUrl: './api-keys-table.component.html',
  styleUrls: ['./api-keys-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ApiKeysTableComponent {

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  active = input<boolean>();
  userId = input<UserId>();

  apiKeysTableConfig: ApiKeysTableConfig;

  constructor(
    private apiKeyService: ApiKeyService,
    private translate: TranslateService,
    private customTranslate: CustomTranslatePipe,
    private dialog: MatDialog,
    private datePipe: DatePipe,
    private cd: ChangeDetectorRef,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
  ) {
    effect(() => {
      if (this.active()) {
        this.apiKeysTableConfig = new ApiKeysTableConfig(
          this.apiKeyService,
          this.translate,
          this.customTranslate,
          this.dialog,
          this.datePipe,
          this.popoverService,
          this.renderer,
          this.viewContainerRef,
          this.userId(),
        );
        this.cd.markForCheck();
      }
    });
  }
}
