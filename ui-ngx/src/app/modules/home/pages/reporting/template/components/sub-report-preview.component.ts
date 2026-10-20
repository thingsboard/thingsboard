// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, inject, ViewEncapsulation } from '@angular/core';
import { SubReportReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { ReportTemplateService } from '@core/http/report-template.service';
import { Observable, of } from 'rxjs';
import { ReportTemplateInfo } from '@shared/models/report.models';
import { catchError, share } from 'rxjs/operators';
import { getEntityDetailsPageURL } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { Router } from '@angular/router';

@Component({
    selector: 'tb-sub-report-preview',
    templateUrl: './sub-report-preview.component.html',
    styleUrls: ['./sub-report-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SubReportPreviewComponent extends AbstractReportComponentPreview<SubReportReportComponentConfig> {

  subReport$: Observable<ReportTemplateInfo>;

  private reportTemplateService = inject(ReportTemplateService);
  private router = inject(Router);

  onComponentUpdated() {
    if (this.reportComponent.templateId !== null) {
      this.subReport$ = this.reportTemplateService
      .getReportTemplateInfo(this.reportComponent.templateId.id, {ignoreLoading: true, ignoreErrors: true}).pipe(
        catchError(() => of(null)),
        share()
      );
    } else {
      this.subReport$ = of(null);
    }
  }

  openSubReportNewTab($event: Event, subReport: ReportTemplateInfo) {
    $event.stopPropagation();
    const subReportUrl = getEntityDetailsPageURL(subReport.id.id, EntityType.REPORT_TEMPLATE);
    const url = this.router.serializeUrl(this.router.createUrlTree([subReportUrl]));
    window.open(url, '_blank');
  }

}
