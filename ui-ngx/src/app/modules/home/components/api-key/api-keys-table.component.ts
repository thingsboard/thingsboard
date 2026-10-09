// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Component({
    selector: 'tb-api-keys-table',
    templateUrl: './api-keys-table.component.html',
    styleUrls: ['./api-keys-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
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
    private userPermissionsService: UserPermissionsService,
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
          this.userPermissionsService,
        );
        this.cd.markForCheck();
      }
    });
  }
}
