// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { SolutionDescriptorAlarm } from '@shared/models/solution-creator.models';
import { alarmSeverityColors } from '@shared/models/alarm.models';

@Component({
  selector: 'tb-alarm-info-panel',
  templateUrl: './alarm-info-panel.component.html',
  styleUrls: ['./entity-info-panel.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class AlarmInfoPanelComponent {

  @Input()
  alarm: SolutionDescriptorAlarm;

  protected readonly Object = Object;

  constructor(private popover: TbPopoverComponent<AlarmInfoPanelComponent>) {
  }

  cancel() {
    this.popover.hide();
  }

  getAlarmSeverityColor(severity: string): string {
    const preparedSeverity: any = severity.toUpperCase();
    return alarmSeverityColors.has(preparedSeverity) ? alarmSeverityColors.get(preparedSeverity) : null;
  }
}
