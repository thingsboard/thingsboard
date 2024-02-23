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

import { Component, HostBinding, Input } from '@angular/core';
import { TooltipPosition } from '@angular/material/tooltip';

@Component({
  selector: '[tb-hint-tooltip-icon]',
  templateUrl: './hint-tooltip-icon.component.html',
  styleUrls: ['./hint-tooltip-icon.component.scss']
})
export class HintTooltipIconComponent {

  @HostBinding('class.tb-hint-tooltip')
  @Input('tb-hint-tooltip-icon') tooltipText: string;

  @Input()
  tooltipPosition: TooltipPosition = 'right';

  @Input()
  hintIcon = 'info';

}
