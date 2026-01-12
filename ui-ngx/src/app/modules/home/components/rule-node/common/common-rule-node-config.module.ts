///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { SharedModule } from '@shared/public-api';
import { HomeComponentsModule } from '@home/components/public-api';
import { KvMapConfigComponent } from './kv-map-config.component';
import { DeviceRelationsQueryConfigComponent } from './device-relations-query-config.component';
import { RelationsQueryConfigComponent } from './relations-query-config.component';
import { MessageTypesConfigComponent } from './message-types-config.component';
import { CredentialsConfigComponent } from './credentials-config.component';
import { ArgumentsMapConfigComponent } from './arguments-map-config.component';
import { MathFunctionAutocompleteComponent } from './math-function-autocomplete.component';
import { OutputMessageTypeAutocompleteComponent } from './output-message-type-autocomplete.component';
import { KvMapConfigOldComponent } from './kv-map-config-old.component';
import { MsgMetadataChipComponent } from './msg-metadata-chip.component';
import { SvMapConfigComponent } from './sv-map-config.component';
import { RelationsQueryConfigOldComponent } from './relations-query-config-old.component';
import { SelectAttributesComponent } from './select-attributes.component';
import { AlarmStatusSelectComponent } from './alarm-status-select.component';
import { ExampleHintComponent } from './example-hint.component';
import { TimeUnitInputComponent } from './time-unit-input.component';

@NgModule({
  declarations: [
    KvMapConfigComponent,
    DeviceRelationsQueryConfigComponent,
    RelationsQueryConfigComponent,
    MessageTypesConfigComponent,
    CredentialsConfigComponent,
    ArgumentsMapConfigComponent,
    MathFunctionAutocompleteComponent,
    OutputMessageTypeAutocompleteComponent,
    KvMapConfigOldComponent,
    MsgMetadataChipComponent,
    SvMapConfigComponent,
    RelationsQueryConfigOldComponent,
    SelectAttributesComponent,
    AlarmStatusSelectComponent,
    ExampleHintComponent,
    TimeUnitInputComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule
  ],
  exports: [
    KvMapConfigComponent,
    DeviceRelationsQueryConfigComponent,
    RelationsQueryConfigComponent,
    MessageTypesConfigComponent,
    CredentialsConfigComponent,
    ArgumentsMapConfigComponent,
    MathFunctionAutocompleteComponent,
    OutputMessageTypeAutocompleteComponent,
    KvMapConfigOldComponent,
    MsgMetadataChipComponent,
    SvMapConfigComponent,
    RelationsQueryConfigOldComponent,
    SelectAttributesComponent,
    AlarmStatusSelectComponent,
    ExampleHintComponent,
    TimeUnitInputComponent
  ]
})

export class CommonRuleNodeConfigModule {
}
