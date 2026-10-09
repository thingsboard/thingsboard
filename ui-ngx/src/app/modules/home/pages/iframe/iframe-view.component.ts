// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { of, Subscription } from 'rxjs';
import { HomeDashboard } from '@shared/models/dashboard.models';
import { Observable } from 'rxjs/internal/Observable';
import { isDefinedAndNotNull } from '@core/utils';
import { map } from 'rxjs/operators';
import { DashboardService } from '@core/http/dashboard.service';

@Component({
  selector: 'tb-iframe-view',
  templateUrl: './iframe-view.component.html',
  standalone: false
})
export class IFrameViewComponent implements OnInit, OnDestroy {

  @HostBinding('style.width') public width = '100%';
  @HostBinding('style.height') public height = '100%';

  iframeUrl: string;
  dashboard: HomeDashboard;
  loading = true;

  private sub: Subscription;

  constructor(private route: ActivatedRoute,
              private dashboardService: DashboardService) {
  }

  ngOnInit(): void {
    this.sub = this.route.queryParams.subscribe((queryParams) => {
      this.iframeUrl = null;
      if (this.isDashboard(queryParams)) {
        const dashboardParams = this.parseDashboardParams(queryParams);
        if (this.dashboard?.id?.id === dashboardParams.dashboardId &&
          this.dashboard?.hideDashboardToolbar === dashboardParams.hideDashboardToolbar) {
          return;
        }
        this.loading = true;
        const prevDashboard = this.dashboard;
        this.dashboard = null;
        this.resolveDashboard(dashboardParams, prevDashboard).subscribe((dashboard) => {
          Promise.resolve().then(() => {
            this.dashboard = dashboard;
            this.loading = false;
          });
        });
      } else {
        this.dashboard = null;
        let iframeUrl: string;
        let setAccessToken: string;
        if (queryParams.childIframeUrl) {
          iframeUrl = queryParams.childIframeUrl;
          setAccessToken = queryParams.childSetAccessToken;
        } else {
          iframeUrl = queryParams.iframeUrl;
          setAccessToken = queryParams.setAccessToken;
        }
        if (setAccessToken === 'true') {
          const accessToken = AuthService.getJwtToken();
          if (iframeUrl.indexOf('?') > -1) {
            iframeUrl += '&';
          } else {
            iframeUrl += '?';
          }
          iframeUrl += `accessToken=${accessToken}`;
        }
        this.iframeUrl = iframeUrl;
        this.loading = false;
      }
    });
  }

  private isDashboard(queryParams: Params): boolean {
    if (queryParams.childDashboardId) {
      return true;
    } else if (queryParams.childIframeUrl) {
      return false;
    } else if (queryParams.dashboardId) {
      return true;
    }
    return false;
  }

  private resolveDashboard(dashboardParams: { dashboardId: string; hideDashboardToolbar: boolean },
                           prevDashboard: HomeDashboard): Observable<HomeDashboard> {
    if (dashboardParams.dashboardId) {
      if (prevDashboard?.id?.id === dashboardParams.dashboardId) {
        return of({...prevDashboard, hideDashboardToolbar: dashboardParams.hideDashboardToolbar});
      } else {
        return this.dashboardService.getDashboard(dashboardParams.dashboardId).pipe(
          map((dashboard) => {
            return {...dashboard, hideDashboardToolbar: dashboardParams.hideDashboardToolbar};
          })
        );
      }
    } else {
      return of(null);
    }
  }

  private parseDashboardParams(queryParams: Params): { dashboardId: string; hideDashboardToolbar: boolean } {
    let dashboardId: string;
    let hideDashboardToolbar: boolean;
    if (queryParams.childDashboardId) {
      dashboardId = queryParams.childDashboardId;
      hideDashboardToolbar = isDefinedAndNotNull(queryParams.childHideDashboardToolbar) ?
        queryParams.childHideDashboardToolbar === 'true' : true;
    } else if (queryParams.dashboardId) {
      dashboardId = queryParams.dashboardId;
      hideDashboardToolbar = isDefinedAndNotNull(queryParams.hideDashboardToolbar) ? queryParams.hideDashboardToolbar === 'true' : true;
    }
    return {dashboardId, hideDashboardToolbar: hideDashboardToolbar || false};
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

}
