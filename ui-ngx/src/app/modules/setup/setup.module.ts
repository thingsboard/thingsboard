// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '@app/shared/shared.module';
import { ActivatePlatformComponent } from '@modules/setup/pages/setup/activate-platform.component';
import { SetupRoutingModule } from '@modules/setup/setup-routing.module';

@NgModule({
  declarations: [
    ActivatePlatformComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    SetupRoutingModule
  ]
})
export class SetupModule { }
