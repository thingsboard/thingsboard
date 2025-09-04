///
/// Copyright © 2016-2025 The Thingsboard Authors
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
