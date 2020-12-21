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
import { AcsComponent } from '@modules/home/pages/acs/acs.component';
import { DialogDataDialog } from '@modules/home/pages/acs/Dialog.component';

import { AcsRoutingModule } from '@modules/home/pages/acs/acs-routing.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { ChartsModule } from 'ng2-charts';


@NgModule({
  declarations: [
   AcsComponent,
   DialogDataDialog
   
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AcsRoutingModule,
    ChartsModule
    
  ]
})
export class ACSModule { }
