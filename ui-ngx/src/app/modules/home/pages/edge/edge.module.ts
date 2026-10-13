// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { EdgeTableHeaderComponent } from '@home/pages/edge/edge-table-header.component';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { AgentSharedComponentsModule } from '@home/components/agent/agent-shared-components.module';
import { EdgeRoutingModule } from '@home/pages/edge/edge-routing.module';
import { EdgeComponent } from './edge.component';
import { EDGE_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { EdgeGroupConfigFactory } from '@home/pages/edge/edge-group-config.factory';
import { EdgeInstructionsDialogComponent } from './edge-instructions-dialog.component';
import { RequestEdgeComponent } from '@home/pages/edge/request-edge.component';

@NgModule({
  declarations: [
    RequestEdgeComponent,
    EdgeComponent,
    EdgeTableHeaderComponent,
    EdgeInstructionsDialogComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AgentSharedComponentsModule,
    HomeDialogsModule,
    EdgeRoutingModule
  ],
  providers: [
    {
      provide: EDGE_GROUP_CONFIG_FACTORY,
      useClass: EdgeGroupConfigFactory
    }
  ]
})

export class EdgeModule { }
