// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ApiUsageRoutingModule } from '@home/pages/api-usage/api-usage-routing.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    ApiUsageRoutingModule
  ]
})
export class ApiUsageModule { }
