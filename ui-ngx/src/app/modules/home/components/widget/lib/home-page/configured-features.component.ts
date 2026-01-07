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
import { TranslateService } from '@ngx-translate/core';
import { MediaBreakpoints } from '@shared/models/constants';

@Component({
  selector: 'tb-configured-features',
  templateUrl: './configured-features.component.html',
  styleUrls: ['./home-page-widget.scss', './configured-features.component.scss']
})
export class ConfiguredFeaturesComponent extends PageComponent implements OnInit, OnDestroy {

  authUser = getCurrentAuthUser(this.store);
  featuresInfo: FeaturesInfo;
  rowHeight = '50px';
  gutterSize = '12px';
  bottomColspan = 2;
  lastColspan = 2;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private adminService: AdminService,
              private translate: TranslateService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    const isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    const isLtMd = this.breakpointObserver.isMatched(MediaBreakpoints['lt-md']);
    this.rowHeight = isMdLg ? '22px' : '50px';
    this.gutterSize = isMdLg ? '8px' : '12px';
    this.bottomColspan = isLtMd ? 3 : 2;
    this.lastColspan = isLtMd ? 6 : 2;
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe([MediaBreakpoints['md-lg'], MediaBreakpoints['lt-md']])
      .subscribe((state: BreakpointState) => {
          if (state.breakpoints[MediaBreakpoints['md-lg']]) {
            this.rowHeight = '22px';
            this.gutterSize = '8px';
          } else {
            this.rowHeight = '50px';
            this.gutterSize = '12px';
          }
          if (state.breakpoints[MediaBreakpoints['lt-md']]) {
            this.bottomColspan = 3;
            this.lastColspan = 6;
          } else {
            this.bottomColspan = 2;
            this.lastColspan = 2;
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
      return this.translate.instant('widgets.configured-features.feature-configured');
    } else {
      return this.translate.instant('widgets.configured-features.feature-not-configured');
    }
  }
}
