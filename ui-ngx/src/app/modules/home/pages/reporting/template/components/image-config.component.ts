// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ViewEncapsulation } from '@angular/core';
import { FormGroup, Validators } from '@angular/forms';
import {
  AbstractReportComponentConfig
} from '@home/pages/reporting/template/components/report-component-config.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { merge } from 'rxjs';
import {
  imageAlignments,
  imageAlignmentTranslations,
  ImageReportComponentConfig,
  imageSourceType,
  imageWidthTypes,
  imageWidthTypeTranslations
} from '@shared/models/report-component.models';
import { WidgetConfigMode } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { getDataKey, updateDataKeys } from '@shared/models/widget-settings.models';

@Component({
    selector: 'tb-image-config',
    templateUrl: './image-config.component.html',
    styleUrls: ['./report-component-config.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ImageConfigComponent extends AbstractReportComponentConfig<ImageReportComponentConfig> {

  imageWidthTypes = imageWidthTypes;
  imageWidthTypeTranslations = imageWidthTypeTranslations;

  imageAlignments = imageAlignments;
  imageAlignmentTranslations = imageAlignmentTranslations;

  basicMode = WidgetConfigMode.basic;

  DataKeyType = DataKeyType;

  settingsTab: 'image' | 'layout' = 'image';

  private initialImageUrl: string;
  private imageWidth: number;

  protected buildForm(reportComponentConfig: ImageReportComponentConfig): FormGroup {
    this.initialImageUrl = reportComponentConfig.imageUrl;
    const form = this.fb.group({
      sourceType: [reportComponentConfig.sourceType || 'image', []],
      imageUrl: [reportComponentConfig.imageUrl, []],
      dataSources: [reportComponentConfig.dataSources, []],
      entityKey: [getDataKey(reportComponentConfig.dataSources), []],
      widthType: [reportComponentConfig.widthType || 'fitWidth', []],
      customWidth: [reportComponentConfig.customWidth || 100, [Validators.min(1)]],
      alignment: [reportComponentConfig.alignment || 'center', []]
    });
    merge(form.get('sourceType').valueChanges, form.get('widthType').valueChanges).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateCustomWidth();
    });
    return form;
  }

  protected prepareOutputConfig(config: any): any {
    updateDataKeys(config.dataSources, [config.entityKey]);
    delete config.entityKey;
    return config;
  }

  private updateCustomWidth() {
    const sourceType: imageSourceType = this.reportConfigForm.get('sourceType').value;
    if (!this.reportConfigForm.get('customWidth').touched) {
      const size = sourceType === 'entityKey' ? 200 : (this.imageWidth || 100);
      this.reportConfigForm.get('customWidth').patchValue(size);
    }
  }

  imageSizeUpdated(size: {width: number, height: number}): void {
    this.imageWidth = size.width;
    if (!this.reportConfigForm.get('customWidth').touched &&
         this.reportConfigForm.get('imageUrl').value !== this.initialImageUrl) {
        this.initialImageUrl = null;
        this.reportConfigForm.get('customWidth').patchValue(size.width);
    }
  }
}
