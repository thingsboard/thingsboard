// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/public-api';
import { CheckMessageConfigComponent } from './check-message-config.component';
import { CheckRelationConfigComponent } from './check-relation-config.component';
import { GpsGeoFilterConfigComponent } from './gps-geo-filter-config.component';
import { MessageTypeConfigComponent } from './message-type-config.component';
import { OriginatorTypeConfigComponent } from './originator-type-config.component';
import { ScriptConfigComponent } from './script-config.component';
import { SwitchConfigComponent } from './switch-config.component';
import { CheckAlarmStatusComponent } from './check-alarm-status.component';
import { CommonRuleNodeConfigModule } from '../common/common-rule-node-config.module';

@NgModule({
  declarations: [
    CheckMessageConfigComponent,
    CheckRelationConfigComponent,
    GpsGeoFilterConfigComponent,
    MessageTypeConfigComponent,
    OriginatorTypeConfigComponent,
    ScriptConfigComponent,
    SwitchConfigComponent,
    CheckAlarmStatusComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    CommonRuleNodeConfigModule
  ],
  exports: [
    CheckMessageConfigComponent,
    CheckRelationConfigComponent,
    GpsGeoFilterConfigComponent,
    MessageTypeConfigComponent,
    OriginatorTypeConfigComponent,
    ScriptConfigComponent,
    SwitchConfigComponent,
    CheckAlarmStatusComponent
  ]
})
export class FilterRuleNodeConfigModule {
}
