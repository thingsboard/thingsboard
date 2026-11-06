// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { NgModule } from '@angular/core';
import { SolutionChatComponent } from '@home/components/ai/solution/solution-chat.component';
import {
  SolutionCreatorOverviewEntitiesComponent
} from '@home/components/ai/solution/overview/solution-creator-overview-entities.component';
import { AlarmInfoPanelComponent } from '@home/components/ai/solution/overview/alarm-info-panel.component';
import { EntityInfoPanelComponent } from '@home/components/ai/solution/overview/entity-info-panel.component';
import { IamInfoPanelComponent } from '@home/components/ai/solution/overview/iam-info-panel.component';
import { MetricInfoPanelComponent } from '@home/components/ai/solution/overview/metric-info-panel.component';
import {
  SolutionCreatorOverviewDashboardComponent
} from '@home/components/ai/solution/overview/solution-creator-overview-dashboard.component';
import {
  SolutionInfoDialogComponent
} from '@home/components/ai/solution/solution-info/solution-info-dialog.component';
import {
  IssuesPopoverComponent
} from '@home/components/ai/solution/solution-info/issues-popover.component';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { AiModule } from '@home/components/ai/ai.module';

@NgModule({
  declarations: [
    SolutionChatComponent,
    SolutionCreatorOverviewEntitiesComponent,
    AlarmInfoPanelComponent,
    EntityInfoPanelComponent,
    MetricInfoPanelComponent,
    IamInfoPanelComponent,
    SolutionCreatorOverviewDashboardComponent,
    SolutionInfoDialogComponent,
    IssuesPopoverComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    NgxGraphModule,
    AiModule
  ],
  exports: [
    SolutionChatComponent
  ]
})
export class SolutionChatModule { }
