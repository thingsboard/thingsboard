// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { CalculatedFieldsRoutingModule } from '@home/pages/calculated-fields/calculated-fields-routing.module';
import { CalculatedFieldsModule } from '@home/components/calculated-fields/calculated-field.module';
import { CalculatedFieldsTabsComponent } from '@home/pages/calculated-fields/calculated-fields-tabs.component';

@NgModule({
  declarations: [
    CalculatedFieldsTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    CalculatedFieldsRoutingModule,
  ]
})
export class CalculatedFieldPageModule { }