///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { FeaturesInfo } from '@shared/models/settings.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';

@Component({
  selector: 'tb-configured-features',
  templateUrl: './configured-features.component.html',
  styleUrls: ['./configured-features.component.scss']
})
export class ConfiguredFeaturesComponent extends PageComponent implements OnInit, OnDestroy {

  authUser = getCurrentAuthUser(this.store);
  featuresInfo: FeaturesInfo;
  rowHeight = '48.5px';
  gutterSize = '12px';

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private adminService: AdminService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    const isMdLg = this.breakpointObserver.isMatched('screen and (min-width: 960px) and (max-width: 1819px)');
    this.rowHeight = isMdLg ? '21.5px' : '48.5px';
    this.gutterSize = isMdLg ? '8px' : '12px';
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe('screen and (min-width: 960px) and (max-width: 1819px)')
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.rowHeight = '21.5px';
            this.gutterSize = '8px';
          } else {
            this.rowHeight = '48.5px';
            this.gutterSize = '12px';
          }
          this.cd.markForCheck();
        }
      );
    (this.authUser.authority === Authority.SYS_ADMIN ?
    this.adminService.getFeaturesInfo() : of(null)).subscribe(
      (featuresInfo) => {
        this.featuresInfo = featuresInfo;
        this.cd.markForCheck();
      }
    );
  }

  ngOnDestroy() {
    if (this.observeBreakpointSubscription) {
      this.observeBreakpointSubscription.unsubscribe();
    }
    super.ngOnDestroy();
  }

  featureTooltip(configured: boolean): string {
    if (configured) {
      return 'Feature is configured.\nClick to setup';
    } else {
      return 'Feature is not configured.\nClick to setup';
    }
  }
}
