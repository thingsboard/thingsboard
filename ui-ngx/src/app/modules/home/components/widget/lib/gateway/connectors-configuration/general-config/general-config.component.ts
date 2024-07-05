///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import {
  ControlContainer,
  FormGroup,
} from '@angular/forms';
import {
  ConnectorType,
  GatewayLogLevel,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'tb-general-config',
  templateUrl: './general-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class GeneralConfigComponent {
  gatewayLogLevel = Object.values(GatewayLogLevel);
  connectorType = ConnectorType;

  get parentFormGroup(): FormGroup {
    return this.parentContainer.control as FormGroup;
  }

  private parentContainer = inject(ControlContainer);
}
