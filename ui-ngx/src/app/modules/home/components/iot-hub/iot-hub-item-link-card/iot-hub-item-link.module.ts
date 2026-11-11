// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { TbIotHubItemLinkCardComponent } from './iot-hub-item-link-card.component';

@NgModule({
  declarations: [TbIotHubItemLinkCardComponent],
  imports: [CommonModule, SharedModule],
  exports: [TbIotHubItemLinkCardComponent]
})
export class IotHubItemLinkModule {}
