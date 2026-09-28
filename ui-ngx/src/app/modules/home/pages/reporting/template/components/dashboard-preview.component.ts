// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, inject, ViewEncapsulation } from '@angular/core';
import { DashboardReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { Observable, of } from 'rxjs';
import { catchError, share } from 'rxjs/operators';
import { DashboardInfo } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-dashboard-preview',
    templateUrl: './dashboard-preview.component.html',
    styleUrls: ['./dashboard-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DashboardPreviewComponent extends AbstractReportComponentPreview<DashboardReportComponentConfig> {

  dashboard$: Observable<DashboardInfo>;

  imageWidth: string = '100%';

  imageAlign: string = 'center';

  private dashboardService = inject(DashboardService);
  private router = inject(Router);

  onComponentUpdated() {
    if (this.reportComponent.config?.dashboardId) {
      this.dashboard$ = this.dashboardService
      .getDashboardInfo(this.reportComponent.config.dashboardId, {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(() => of(null)),
        share()
      );
    } else {
      this.dashboard$ = of(null);
    }
    this.imageWidth = '100%';
    if (this.reportComponent.widthType === 'original') {
      this.imageWidth = 'auto';
    } else if (this.reportComponent.widthType === 'custom') {
      const customWidth = this.reportComponent.customWidth || 100;
      this.imageWidth = customWidth + 'px';
    }
    this.imageAlign = this.reportComponent.alignment || 'center';
  }

  openDashboardNewTab($event: Event, dashboard: DashboardInfo) {
    $event.stopPropagation();
    const dashboardUrl = getEntityDetailsPageURL(dashboard.id.id, EntityType.DASHBOARD);
    const url = this.router.serializeUrl(this.router.createUrlTree([dashboardUrl]));
    window.open(url, '_blank');
  }

}
