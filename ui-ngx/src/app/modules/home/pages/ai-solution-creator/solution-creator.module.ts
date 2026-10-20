// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { NgModule } from '@angular/core';
import { SolutionCreatorComponent } from '@home/pages/ai-solution-creator/solution-creator.component';
import { SolutionCreatorRoutingModule } from '@home/pages/ai-solution-creator/solution-creator-routing.module';
import {
  SolutionCreatorOverviewEntitiesComponent
} from '@home/pages/ai-solution-creator/overview/solution-creator-overview-entities.component';
import { AlarmInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/alarm-info-panel.component';
import { EntityInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/entity-info-panel.component';
import { IamInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/iam-info-panel.component';
import { MetricInfoPanelComponent } from '@home/pages/ai-solution-creator/overview/metric-info-panel.component';
import {
  SolutionCreatorOverviewDashboardComponent
} from '@home/pages/ai-solution-creator/overview/solution-creator-overview-dashboard.component';
import { SolutionSideBarComponent } from '@home/pages/ai-solution-creator/side-bar/solution-side-bar.component';
import {
  SolutionInfoDialogComponent
} from '@home/pages/ai-solution-creator/solution-info/solution-info-dialog.component';
import {
  IssuesPopoverComponent
} from '@home/pages/ai-solution-creator/solution-info/issues-popover.component';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { AiModule } from '@home/components/ai/ai.module';

@NgModule({
  declarations: [
    SolutionCreatorComponent,
    SolutionCreatorOverviewEntitiesComponent,
    AlarmInfoPanelComponent,
    EntityInfoPanelComponent,
    MetricInfoPanelComponent,
    IamInfoPanelComponent,
    SolutionCreatorOverviewDashboardComponent,
    SolutionSideBarComponent,
    SolutionInfoDialogComponent,
    IssuesPopoverComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    SolutionCreatorRoutingModule,
    NgxGraphModule,
    AiModule
  ]
})
export class SolutionCreatorModule { }
