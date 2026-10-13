// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { AiMetricType, SolutionDescriptorMetric } from '@shared/models/solution-creator.models';
import { CalculatedFieldTypeTranslations } from '@shared/models/calculated-field.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-metric-info-panel',
  templateUrl: './metric-info-panel.component.html',
  styleUrls: ['./entity-info-panel.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class MetricInfoPanelComponent {

  @Input()
  metric: SolutionDescriptorMetric;

  constructor(private popover: TbPopoverComponent<MetricInfoPanelComponent>,
              private translate: TranslateService) {
  }

  cancel() {
    this.popover.hide();
  }

  getMetricTypeName(value: keyof AiMetricType): string {
    if (AiMetricType[value]) {
      return this.translate.instant(CalculatedFieldTypeTranslations.get(AiMetricType[value]).name);
    }
    return value as any;
  }
}
