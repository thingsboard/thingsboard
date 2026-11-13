// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { CommonRuleNodeConfigModule } from '@home/components/rule-node/common/common-rule-node-config.module';

import { AggregateIncomingConfigComponent } from './aggregate-incoming-config.component';
import { AggregateLatestConfigComponent } from './aggregate-latest-config.component';
import { AggregateLatestMappingTableComponent } from './aggregate-latest-mapping-table.component';
import { AggregateLatestMappingDialogComponent } from './aggregate-latest-mapping-dialog.component';
import { AlarmsCountConfigComponent } from './alarms-count-config.component';
import { AlarmsCountMappingTableComponent } from './alarms-count-mapping-table.component';
import { AlarmsCountMappingDialogComponent } from './alarms-count-mapping-dialog.component';
import { AlarmsCountV2ConfigComponent } from './alarms-count-v2-config.component';
import { AggregateLatestV2ConfigComponent } from './aggregate-latest-v2-config.component';

@NgModule({
  declarations: [
    AggregateIncomingConfigComponent,
    AggregateLatestMappingDialogComponent,
    AggregateLatestMappingTableComponent,
    AggregateLatestConfigComponent,
    AlarmsCountMappingDialogComponent,
    AlarmsCountMappingTableComponent,
    AlarmsCountConfigComponent,
    AlarmsCountV2ConfigComponent,
    AggregateLatestV2ConfigComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    AggregateIncomingConfigComponent,
    AggregateLatestConfigComponent,
    AlarmsCountConfigComponent,
    AlarmsCountV2ConfigComponent,
    AggregateLatestV2ConfigComponent
  ]
})

export class AnalyticsRuleNodeConfigModule {
}
