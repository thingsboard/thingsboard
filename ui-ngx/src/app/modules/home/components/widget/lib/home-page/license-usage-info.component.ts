// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { LicenseUsageInfo } from '@shared/models/settings.models';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { TranslateService } from '@ngx-translate/core';
import { MediaBreakpoints } from '@shared/models/constants';

@Component({
    selector: 'tb-license-usage-info',
    templateUrl: './license-usage-info.component.html',
    styleUrls: ['./license-usage-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LicenseUsageInfoComponent extends PageComponent implements OnInit, OnDestroy {

  authUser = getCurrentAuthUser(this.store);
  licenseUsageInfo: LicenseUsageInfo;
  isMdLg = false;
  authState = getCurrentAuthState(this.store);
  showEdges = this.authState.licenseVersion > 1 && this.authState.edgesSupportEnabled && this.authState.edgeEnabled;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private adminService: AdminService,
              private translate: TranslateService,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    this.isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['md-lg'])
      .subscribe((state: BreakpointState) => {
        this.isMdLg = state.matches;
        this.cd.markForCheck();
      });
    (this.authUser.authority === Authority.SYS_ADMIN ?
      this.adminService.getLicenseUsageInfo() : of(null)).subscribe(
      (licenseUsageInfo) => {
        this.licenseUsageInfo = licenseUsageInfo;
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

  statusIcon(count?: number, max?: number): string {
    let icon = 'check';
    if (count && max) {
      const percent = (count / max) * 100;
      if (percent > 85) {
        icon = 'warning';
      }
    }
    return icon;
  }

  statusClass(count?: number, max?: number): string {
    let className = 'ok';
    if (count && max) {
      const percent = (count / max) * 100;
      if (percent > 85) {
        className = 'critical';
      }
    }
    return className;
  }
}
