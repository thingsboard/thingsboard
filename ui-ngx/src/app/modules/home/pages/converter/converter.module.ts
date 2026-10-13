// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ConverterTabsComponent } from '@home/pages/converter/converter-tabs.component';
import { ConverterRoutingModule } from '@home/pages/converter/converter-routing.module';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';

@NgModule({
  declarations: [
    ConverterTabsComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeDialogsModule,
    ConverterRoutingModule
  ],
  providers: [EntityDebugSettingsService]
})
export class ConverterModule { }
