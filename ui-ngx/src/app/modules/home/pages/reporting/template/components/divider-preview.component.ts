// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { BorderLength, BorderType, DividerReportComponentConfig } from '@shared/models/report-component.models';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { isDefinedAndNotNull } from '@core/utils';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';

@Component({
    selector: 'tb-report-divider-preview',
    templateUrl: './divider-preview.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class DividerPreviewComponent extends AbstractReportComponentPreview<DividerReportComponentConfig> {

  dividerStyle: ComponentStyle;

  onComponentUpdated() {
    this.dividerStyle = {};
    this.dividerStyle.width = this.reportComponent.length === BorderLength.SHORT ? '50%' : '100%';
    const borderWidth = isDefinedAndNotNull(this.reportComponent.widthPx) ? this.reportComponent.widthPx + 'px' : '1px';
    const borderColor = this.reportComponent.color || '#000';
    const borderStyle = this.reportComponent.borderType || BorderType.solid;
    this.dividerStyle.borderBottom = `${borderWidth} ${borderColor} ${borderStyle}`;
  }

}
