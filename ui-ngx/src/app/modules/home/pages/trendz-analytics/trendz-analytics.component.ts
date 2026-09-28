// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { getMetricLink, TrendzSummary, TrendzViewType } from '@shared/models/trendz-analytics.models';
import { TrendzService } from '@app/core/http/trendz.service';
import { ActivatedRoute } from '@angular/router';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestTrendzComponent } from '@home/pages/trendz-analytics/request-trendz.component';
import { TrendzAnalyticsUnavailableComponent } from './trendz-analytics-unavailable.component';
import { DynamicMatDialog } from '@app/shared/components/dialog/dynamic/dynamic-dialog';

@Component({
    selector: 'tb-trendz-analytics',
    templateUrl: './trendz-analytics.component.html',
    styleUrls: ['./trendz-analytics.component.scss'],
    standalone: false
})
export class TrendzAnalyticsComponent extends PageComponent implements OnInit {

  @ViewChild('replaceComponentAnchor', {static: true}) replaceComponentAnchor: TbAnchorComponent;

  authState = getCurrentAuthState(this.store);
  trendzEnabled = this.authState.licenseVersion < 2 || this.authState.trendzEnabled;

  trendzSummary: TrendzSummary;
  trendzSynced = this.route.snapshot.data.trendzSynced;
  trendzViewTypes = Object.entries(TrendzViewType).map(([key, value]) => ({ key, value }));
  getMetricLink = getMetricLink;

  constructor(protected store: Store<AppState>,
              private trendzService: TrendzService,
              private route: ActivatedRoute,
              private dialog: DynamicMatDialog,
              private elementRef: ElementRef) {
    super();
  }

  ngOnInit(): void {
    if (!this.trendzEnabled) {
      const viewContainerRef = this.replaceComponentAnchor.viewContainerRef;
      viewContainerRef.clear();
      viewContainerRef.createComponent(RequestTrendzComponent);
    } else if(!this.trendzSynced) {
      this.dialog.open(TrendzAnalyticsUnavailableComponent, {
        containerElement: this.elementRef.nativeElement,
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog-lt-lg'],
      });
    } else {
      this.trendzService.getTrendzSummary().subscribe(trendzSummary => {
        if (trendzSummary) {
          this.trendzSummary = trendzSummary;
        }
      });
    }
  }
}
