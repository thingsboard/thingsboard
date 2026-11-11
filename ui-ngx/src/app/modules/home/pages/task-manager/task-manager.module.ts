// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { TaskManagerRoutingModule } from '@home/pages/task-manager/task-manager-routing.module';
import { TaskManagerHeaderComponent } from '@home/pages/task-manager/task-manager-header.component';
import { WidgetConfigComponentsModule } from '@home/components/widget/config/widget-config-components.module';
import { TaskFilterConfigComponent } from '@home/pages/task-manager/task-filter-config.component';
import { TaskInfoPanelComponent } from '@home/pages/task-manager/task-info-panel.component';
import { TaskParametersPanelComponent } from '@home/pages/task-manager/task-parameters-panel.component';

@NgModule({
  declarations: [
    TaskManagerHeaderComponent,
    TaskFilterConfigComponent,
    TaskInfoPanelComponent,
    TaskParametersPanelComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    TaskManagerRoutingModule,
    WidgetConfigComponentsModule
  ]
})
export class TaskManagerModule { }
