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

import { Pipe, PipeTransform } from '@angular/core';
import { PortLimits } from '@home/components/widget/lib/gateway/gateway-widget.models';
import { AbstractControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

@Pipe({
  name: 'getGatewayPortTooltip',
  standalone: true,
})
export class GatewayPortTooltipPipe implements PipeTransform {

  constructor(private translate: TranslateService) {}

  transform(portControl: AbstractControl): string {
    if (portControl.hasError('required')) {
      return this.translate.instant('gateway.port-required');
    }
    if (portControl.hasError('min') || portControl.hasError('max')) {
      return this.translate.instant('gateway.port-limits-error', {
        min: PortLimits.MIN,
        max: PortLimits.MAX,
      });
    }
    return '';
  }
}
