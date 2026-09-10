// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { StatesControllerService } from '@home/components/dashboard-page/states/states-controller.service';
import { EntityStateControllerComponent } from '@home/components/dashboard-page/states/entity-state-controller.component';
import { StatesComponentDirective } from '@home/components/dashboard-page/states/states-component.directive';
import { HomeDialogsModule } from '@app/modules/home/dialogs/home-dialogs.module';
import { DefaultStateControllerComponent } from '@home/components/dashboard-page/states/default-state-controller.component';

@NgModule({
  declarations: [
    StatesComponentDirective,
    DefaultStateControllerComponent,
    EntityStateControllerComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeDialogsModule
  ],
  exports: [
    StatesComponentDirective
  ],
  providers: [
    StatesControllerService
  ]
})
export class StatesControllerModule {

  constructor(private statesControllerService: StatesControllerService) {
    this.statesControllerService.registerStatesController('default', DefaultStateControllerComponent);
    this.statesControllerService.registerStatesController('entity', EntityStateControllerComponent);
  }
}
