// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { environment as env } from '@env/environment';

@Component({
  selector: 'tb-iot-hub-alarm-rules-unavailable-page',
  standalone: false,
  templateUrl: './iot-hub-alarm-rules-unavailable-page.component.html',
  styleUrls: ['./iot-hub-alarm-rules-unavailable-page.component.scss']
})
export class TbIotHubAlarmRulesUnavailablePageComponent {

  // Same colour as the ALARM_RULE HeroTypeConfig entry on the home page.
  readonly alarmRulesColor = '#d66f2e';

  readonly currentTbVersion: string = env.tbVersion;

  constructor(private router: Router) {}

  goBack(): void {
    void this.router.navigate(['/iot-hub']);
  }

  upgradeInstance(): void {
    window.open('https://thingsboard.io/docs/installation/upgrade-instructions/', '_blank');
  }
}
