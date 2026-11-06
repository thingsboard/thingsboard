// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { NgModule } from '@angular/core';
import { SolutionChatComponent } from '@home/components/ai/solution/solution-chat.component';
import {
  SolutionInfoDialogComponent
} from '@home/components/ai/solution/solution-info/solution-info-dialog.component';
import {
  IssuesPopoverComponent
} from '@home/components/ai/solution/solution-info/issues-popover.component';
import { NgxGraphModule } from '@swimlane/ngx-graph';
import { SolutionOverviewComponent } from '@home/components/ai/solution/pane/solution-overview.component';
import { SolutionDashboardsComponent } from '@home/components/ai/solution/pane/solution-dashboards.component';
import { SolutionInstalledComponent } from '@home/components/ai/solution/pane/solution-installed.component';
import { AiModule } from '@home/components/ai/ai.module';

@NgModule({
  declarations: [
    SolutionChatComponent,
    SolutionOverviewComponent,
    SolutionDashboardsComponent,
    SolutionInstalledComponent,
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
