// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { EmptyPageComponent } from '@modules/empty-page/empty-page.component';
import { EmptyPageRotingModule } from '@modules/empty-page/empty-page-routing.module';

@NgModule({
  declarations: [
    EmptyPageComponent
  ],
  imports: [
    EmptyPageRotingModule
  ]
})
export class EmptyPageModule {}
