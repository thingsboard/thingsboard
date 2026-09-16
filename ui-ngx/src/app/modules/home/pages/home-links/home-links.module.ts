// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HomeLinksRoutingModule } from './home-links-routing.module';
import { HomeLinksComponent } from './home-links.component';
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';

@NgModule({
  declarations:
    [
      HomeLinksComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    HomeLinksRoutingModule
  ]
})
export class HomeLinksModule { }
