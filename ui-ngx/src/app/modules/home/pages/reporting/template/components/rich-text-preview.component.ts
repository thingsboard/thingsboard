// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild, ViewEncapsulation } from '@angular/core';
import { RichTextReportComponentConfig } from '@shared/models/report-component.models';
import { AbstractReportComponentPreview } from '@home/pages/reporting/template/components/report-component.component';
import {
  extractKeyFromVariable,
  imagePlaceholder,
  isKeyVariable,
  keyImage
} from '@home/pages/reporting/template/components/report-component.models';
import { of } from 'rxjs';

@Component({
    selector: 'tb-rich-text-preview',
    templateUrl: './rich-text-preview.component.html',
    styleUrls: ['./rich-text-preview.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class RichTextPreviewComponent extends AbstractReportComponentPreview<RichTextReportComponentConfig> implements AfterViewInit, OnDestroy {

  richTextEl = viewChild('richText', {
    read: ElementRef<HTMLElement>,
  });

  private richTextResize$: ResizeObserver;

  html: string;

  htmlWithImageOptions =  {
    customImageUrlCallback: (url: string)=> {
      if (!url) {
        return of(imagePlaceholder);
      } else if (isKeyVariable(url)) {
        const key = extractKeyFromVariable(url);
        return of(keyImage(key));
      } else {
        return null;
      }
    }
  };

  onComponentUpdated() {
    if (this.reportComponent.value && this.reportComponent.value.trim().length) {
      this.html = this.reportComponent.value;
    } else {
      this.html = '<p>&nbsp;</p>';
    }
  }

  ngAfterViewInit() {
    this.richTextResize$ = new ResizeObserver(() => {
      this.contentResized.emit();
    });
    this.richTextResize$.observe(this.richTextEl().nativeElement);
  }

  ngOnDestroy() {
    if (this.richTextResize$) {
      this.richTextResize$.disconnect();
    }
  }

}
