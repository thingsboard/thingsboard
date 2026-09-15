// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  effect,
  input,
  Renderer2,
  ViewChild,
} from '@angular/core';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AlarmRulesService } from '@core/http/alarm-rules.service';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { DatePipe } from '@angular/common';
import { AlarmRulesTableConfig } from "@home/components/alarm-rules/alarm-rules-table-config";
import { UtilsService } from "@core/services/utils.service";
import { ActivatedRoute, Router } from "@angular/router";
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';

@Component({
    selector: 'tb-alarm-rules-table',
    templateUrl: './alarm-rules-table.component.html',
    styleUrls: ['./alarm-rules-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [EntityDebugSettingsService],
    standalone: false
})
export class AlarmRulesTableComponent {

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  active = input<boolean>();
  entityId = input<EntityId>();
  entityName = input<string>();
  ownerId = input<EntityId>();

  alarmRulesTableConfig: AlarmRulesTableConfig;

  pageMode: boolean = false;

  constructor(private alarmRulesService: AlarmRulesService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private datePipe: DatePipe,
              private cd: ChangeDetectorRef,
              private renderer: Renderer2,
              private importExportService: ImportExportService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private utilsService: UtilsService,
              private destroyRef: DestroyRef,
              private route: ActivatedRoute,
              private router: Router,
              private iotHubActions: IotHubActionsService
  ) {
    this.pageMode = !!this.route.snapshot.data.isPage;
    effect(() => {
      if (this.active() || this.pageMode) {
        this.alarmRulesTableConfig = new AlarmRulesTableConfig(
          this.alarmRulesService,
          this.translate,
          this.dialog,
          this.datePipe,
          this.entityId(),
          this.store,
          this.destroyRef,
          this.renderer,
          this.entityName(),
          this.ownerId(),
          this.importExportService,
          this.entityDebugSettingsService,
          this.utilsService,
          this.router,
          this.iotHubActions,
          this.pageMode,
        );
        this.cd.markForCheck();
      }
    });
  }
}
