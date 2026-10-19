// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild, ViewEncapsulation } from '@angular/core';
import { ImageReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import { getDataKey } from '@shared/models/widget-settings.models';
import { imagePlaceholder, keyImage } from '@home/pages/reporting/template/components/report-component.models';
import { isNotEmptyStr } from '@core/utils';

@Component({
    selector: 'tb-image-preview',
    templateUrl: './image-preview.component.html',
    styleUrls: ['./image-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ImagePreviewComponent extends AbstractReportComponentPreview<ImageReportComponentConfig> implements AfterViewInit, OnDestroy {

  imageEl = viewChild('image', {
    read: ElementRef<HTMLElement>,
  });

  private imageResize$: ResizeObserver;

  imageUrl: string;

  imageWidth: string = '100%';

  imageAlign: string = 'center';

  imagePlaceholder = imagePlaceholder;

  triggerUpdate: number = 0;

  onComponentUpdated() {
    if (this.reportComponent.sourceType === 'entityKey') {
      const key = getDataKey(this.reportComponent.dataSources);
      if (key) {
        this.imageUrl = keyImage(key.name);
      } else {
        this.imageUrl = this.imagePlaceholder;
      }
    } else {
      if (isNotEmptyStr(this.reportComponent.imageUrl)) {
        if (this.imageUrl === this.reportComponent.imageUrl) {
          this.triggerUpdate +=1;
        } else {
          this.imageUrl = this.reportComponent.imageUrl;
        }
      } else {
        this.imageUrl = this.imagePlaceholder;
      }
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

  ngAfterViewInit() {
    this.imageResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.imageResize$.observe(this.imageEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.imageResize$) {
      this.imageResize$.disconnect();
    }
  }

}
