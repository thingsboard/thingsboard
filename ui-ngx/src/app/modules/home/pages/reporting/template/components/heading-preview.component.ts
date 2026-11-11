// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { HeadingReportComponentConfig } from '@shared/models/report-component.models';
import { ComponentStyle, Font, textStyle } from '@shared/models/widget-settings.models';
import { deepClone } from '@core/utils';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';

@Component({
    selector: 'tb-report-heading-preview',
    templateUrl: './heading-preview.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class HeadingPreviewComponent extends AbstractReportComponentPreview<HeadingReportComponentConfig> {

  headingStyle: ComponentStyle;

  height: string;

  text: string;

  onComponentUpdated() {
    if (this.reportComponent.value && this.reportComponent.value.trim().length) {
      this.text = this.reportComponent.value;
    } else {
      this.text = '&nbsp;';
    }
    const font: Font = deepClone(this.reportComponent.font || { size: 10, sizeUnit: 'pt' } as Font);
    if (!font.size) {
      font.size = 10;
    }
    if (font.sizeUnit !== 'pt') {
      font.sizeUnit = 'pt';
    }
    this.headingStyle = textStyle(font);
    this.headingStyle.color = this.reportComponent.color || '#000';
    if (this.reportComponent.textAlignment) {
      this.headingStyle.textAlign = this.reportComponent.textAlignment;
    }
    if (this.reportComponent.verticalAlignment) {
      this.headingStyle.verticalAlign = this.reportComponent.verticalAlignment;
    }
    if (this.reportComponent.height) {
      this.height = this.reportComponent.height + 'pt';
    } else {
      this.height = '100%';
    }
  }

}
