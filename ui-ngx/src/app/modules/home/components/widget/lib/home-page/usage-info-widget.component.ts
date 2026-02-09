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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { of } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { UsageInfo } from '@shared/models/usage.models';
import { UsageInfoService } from '@core/http/usage-info.service';
import { ShortNumberPipe } from '@shared/pipe/short-number.pipe';

@Component({
    selector: 'tb-usage-info-widget',
    templateUrl: './usage-info-widget.component.html',
    styleUrls: ['./home-page-widget.scss', './usage-info-widget.component.scss'],
    standalone: false
})
export class UsageInfoWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  usageInfo: UsageInfo;
  authUser = getCurrentAuthUser(this.store);

  toggleValue: 'entities' | 'apiCalls' = 'entities';

  entityItemCritical: {[key: string]: boolean} = {};
  apiCallItemCritical: {[key: string]: boolean} = {};

  hasUsageInfoAccess = true;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private shortNumberPipe: ShortNumberPipe,
              private usageInfoService: UsageInfoService) {
    super(store);
  }

  ngOnInit() {
    this.hasUsageInfoAccess = this.authUser.authority === Authority.TENANT_ADMIN;
    if (this.hasUsageInfoAccess) {
      this.usageInfoService.getUsageInfo().subscribe(
        (usageInfo) => {
          this.usageInfo = usageInfo;
          this.entityItemCritical.devices = this.isItemCritical(this.usageInfo?.devices, this.usageInfo?.maxDevices);
          this.entityItemCritical.assets = this.isItemCritical(this.usageInfo?.assets, this.usageInfo?.maxAssets);
          this.entityItemCritical.users = this.isItemCritical(this.usageInfo?.users, this.usageInfo?.maxUsers);
          this.entityItemCritical.dashboards = this.isItemCritical(this.usageInfo?.dashboards, this.usageInfo?.maxDashboards);
          this.entityItemCritical.customers = this.isItemCritical(this.usageInfo?.customers, this.usageInfo?.maxCustomers);
          this.apiCallItemCritical.transportMessages = this.isItemCritical(this.usageInfo?.transportMessages,
            this.usageInfo?.maxTransportMessages);
          this.apiCallItemCritical.jsExecutions = this.isItemCritical(this.usageInfo?.jsExecutions, this.usageInfo?.maxJsExecutions);
          this.apiCallItemCritical.alarms = this.isItemCritical(this.usageInfo?.alarms, this.usageInfo?.maxAlarms);
          this.apiCallItemCritical.emails = this.isItemCritical(this.usageInfo?.emails, this.usageInfo?.maxEmails);
          this.apiCallItemCritical.sms = this.isItemCritical(this.usageInfo?.sms, this.usageInfo?.maxSms);
          let entitiesHasCriticalItem = false;
          let apiCallsHasCriticalItem = false;
          for (const key of Object.keys(this.entityItemCritical)) {
            if (this.entityItemCritical[key]) {
              entitiesHasCriticalItem = true;
              break;
            }
          }
          for (const key of Object.keys(this.apiCallItemCritical)) {
            if (this.apiCallItemCritical[key]) {
              apiCallsHasCriticalItem = true;
              break;
            }
          }
          if (apiCallsHasCriticalItem && !entitiesHasCriticalItem) {
            this.toggleValue = 'apiCalls';
          }
          this.cd.markForCheck();
        }
      );
    }
  }

  maxValue(max: number): number | string {
    return max ? this.shortNumberPipe.transform(max) : '∞';
  }

  progressValue(value: number, max: number): number {
    if (max && value) {
      return (value / max) * 100;
    }
    return 0;
  }

  private isItemCritical(value: number, max: number): boolean {
    if (max && value) {
      return (value / max) >= 0.85;
    } else {
      return false;
    }
  }

}
