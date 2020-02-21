///
/// Copyright © 2016-2020 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { StatesControllerService } from './states-controller.service';
import { EntityStateControllerComponent } from './entity-state-controller.component';
import { StatesComponentDirective } from './states-component.directive';
import { HomeDialogsModule } from '@app/modules/home/dialogs/home-dialogs.module';
import { DefaultStateControllerComponent } from '@home/pages/dashboard/states/default-state-controller.component';

@NgModule({
  declarations: [
    StatesComponentDirective,
    DefaultStateControllerComponent,
    EntityStateControllerComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
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
