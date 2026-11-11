// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { EntityViewComponent } from '@modules/home/pages/entity-view/entity-view.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ENTITY_VIEW_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { EntityViewGroupConfigFactory } from '@home/pages/entity-view/entity-view-group-config.factory';
import { EntityViewTableHeaderComponent } from '@home/pages/entity-view/entity-view-table-header.component';
import { EntityViewRoutingModule } from '@home/pages/entity-view/entity-view-routing.module';

@NgModule({
  declarations: [
    EntityViewComponent,
    EntityViewTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    EntityViewRoutingModule
  ],
  providers: [
    {
      provide: ENTITY_VIEW_GROUP_CONFIG_FACTORY,
      useClass: EntityViewGroupConfigFactory
    }
  ]
})
export class EntityViewModule { }
