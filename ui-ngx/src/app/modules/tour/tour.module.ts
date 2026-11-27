// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { WelcomeInvitationComponent } from '@modules/tour/welcome-invitation.component';
import { TourHostComponent } from '@modules/tour/tour-host.component';
import { TourLauncher } from '@modules/tour/tour-launcher.service';

export { TourLauncher } from '@modules/tour/tour-launcher.service';

@NgModule({
  declarations: [
    WelcomeInvitationComponent,
    TourHostComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  providers: [
    TourLauncher
  ]
})
export class TourModule { }
