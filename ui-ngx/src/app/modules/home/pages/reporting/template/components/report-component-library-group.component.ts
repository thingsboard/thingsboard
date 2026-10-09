// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, HostBinding, Input, OnChanges, SimpleChanges,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbReportFormat } from '@shared/models/report.models';
import {
  ReportComponentContext,
  ReportComponentLibraryGroup,
  reportComponentLibraryGroupTranslations
} from '@home/pages/reporting/template/components/report-component.models';
import { MatExpansionPanel } from '@angular/material/expansion';

@Component({
    selector: 'tb-report-component-library-group',
    templateUrl: './report-component-library-group.component.html',
    styleUrls: ['./report-component-library-group.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentLibraryGroupComponent implements OnChanges {

  reportComponentLibraryGroupTranslations = reportComponentLibraryGroupTranslations;

  expansionPanel = viewChild('expansionPanel', {
    read: MatExpansionPanel,
  });

  @Input()
  context: ReportComponentContext;

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  @Input()
  group: ReportComponentLibraryGroup;

  @Input()
  filter: string;

  constructor() {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if ('filter' === propName) {
          this.expansionPanel()?.open();
        }
      }
    }
  }

}
