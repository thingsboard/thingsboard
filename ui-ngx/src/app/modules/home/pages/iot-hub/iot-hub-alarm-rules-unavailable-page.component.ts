// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { environment as env } from '@env/environment';
import { ItemType, itemTypeColors } from '@shared/models/iot-hub/iot-hub-item.models';

@Component({
  selector: 'tb-iot-hub-alarm-rules-unavailable-page',
  standalone: false,
  templateUrl: './iot-hub-alarm-rules-unavailable-page.component.html',
  styleUrls: ['./iot-hub-alarm-rules-unavailable-page.component.scss']
})
export class TbIotHubAlarmRulesUnavailablePageComponent {

  readonly alarmRulesColor = itemTypeColors[ItemType.ALARM_RULE];

  readonly currentTbVersion: string = env.tbVersion;

  constructor(private router: Router) {}

  goBack(): void {
    void this.router.navigate(['/iot-hub']);
  }

  upgradeInstance(): void {
    window.open('https://thingsboard.io/docs/installation/upgrade-instructions/', '_blank');
  }
}
