// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { IntegrationComponent } from '@home/pages/integration/integration.component';
import { IntegrationTabsComponent } from '@home/pages/integration/integration-tabs.component';
import { IntegrationRoutingModule } from '@home/pages/integration/integration-routing.module';
import { IntegrationComponentModule } from '@home/components/integration/integration-component.module';
import { EntityDebugSettingsButtonComponent } from '@home/components/entity/debug/entity-debug-settings-button.component';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';

@NgModule({
  declarations: [
    IntegrationComponent,
    IntegrationTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    IntegrationRoutingModule,
    IntegrationComponentModule,
    EntityDebugSettingsButtonComponent,
  ],
  providers: [EntityDebugSettingsService]
})
export class IntegrationModule { }
