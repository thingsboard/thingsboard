// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { IFrameViewRoutingModule } from '@home/pages/iframe/iframe-view-routing.module';
import { IFrameViewComponent } from '@home/pages/iframe/iframe-view.component';

@NgModule({
  declarations:
    [
      IFrameViewComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    IFrameViewRoutingModule
  ]
})
export class IFrameViewModule { }
