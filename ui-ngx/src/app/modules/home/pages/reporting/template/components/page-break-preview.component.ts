// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { PageBreakReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';

@Component({
    selector: 'tb-page-break-preview',
    templateUrl: './page-break-preview.component.html',
    styleUrls: ['./page-break-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class PageBreakPreviewComponent extends AbstractReportComponentPreview<PageBreakReportComponentConfig> {

  onComponentUpdated() {
  }

}
