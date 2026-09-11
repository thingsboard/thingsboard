// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { EntityViewComponent } from '@modules/home/pages/entity-view/entity-view.component';
import { EntityViewTableHeaderComponent } from './entity-view-table-header.component';
import { EntityViewRoutingModule } from './entity-view-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { EntityViewTabsComponent } from '@home/pages/entity-view/entity-view-tabs.component';

@NgModule({
  declarations: [
    EntityViewComponent,
    EntityViewTabsComponent,
    EntityViewTableHeaderComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    EntityViewRoutingModule
  ]
})
export class EntityViewModule { }
