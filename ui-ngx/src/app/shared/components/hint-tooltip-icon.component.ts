// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, HostBinding, Input } from '@angular/core';
import { TooltipPosition } from '@angular/material/tooltip';

@Component({
    selector: '[tb-hint-tooltip-icon]',
    templateUrl: './hint-tooltip-icon.component.html',
    styleUrls: ['./hint-tooltip-icon.component.scss'],
    standalone: false
})
export class HintTooltipIconComponent {

  @HostBinding('class.tb-hint-tooltip')
  @Input('tb-hint-tooltip-icon') tooltipText: string;

  @Input()
  tooltipPosition: TooltipPosition = 'right';

  @Input()
  hintIcon = 'info_outline';

}
