// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { Heading, HeadingReportComponentConfig } from '@shared/models/report-component.models';

@Component({
    selector: 'tb-report-heading-config',
    templateUrl: './heading-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class HeadingConfigComponent extends AbstractReportComponentConfig<HeadingReportComponentConfig> {

  settingsTab: 'content' | 'data' | 'layout' = 'content';

  protected buildForm(reportComponentConfig: HeadingReportComponentConfig): FormGroup {
    return this.fb.group({
      heading: [this.getHeading(reportComponentConfig), []],
      dataSources: [reportComponentConfig.dataSources, []]
    });
  }


  protected prepareOutputConfig(config: any): HeadingReportComponentConfig {
    const heading: Heading = config.heading;
    this.setHeading(config, heading);
    delete config.heading;
    return config;
  }

  private getHeading(reportComponentConfig: HeadingReportComponentConfig): Heading {
    return {
      text: reportComponentConfig.value,
      font: reportComponentConfig.font,
      color: reportComponentConfig.color,
      textAlignment: reportComponentConfig.textAlignment,
      verticalAlignment: reportComponentConfig.verticalAlignment,
      height: reportComponentConfig.height
    }
  }

  private setHeading(reportComponentConfig: HeadingReportComponentConfig, heading: Heading) {
    reportComponentConfig.value = heading.text;
    reportComponentConfig.font = heading.font;
    reportComponentConfig.color = heading.color;
    reportComponentConfig.textAlignment = heading.textAlignment;
    reportComponentConfig.verticalAlignment = heading.verticalAlignment;
    reportComponentConfig.height = heading.height;
  }

}
