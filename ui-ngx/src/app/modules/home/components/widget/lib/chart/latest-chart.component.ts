///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  LatestChartDataItem,
  LatestChartLegendItem,
  LatestChartSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle, textStyle } from '@shared/models/widget-settings.models';
import { TbLatestChart } from '@home/components/widget/lib/chart/latest-chart';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { TranslateService } from '@ngx-translate/core';
import { LegendPosition } from '@shared/models/widget.models';

export interface LatestChartComponentCallbacks {
  createChart: (chartShape: ElementRef<HTMLElement>, renderer: Renderer2) => TbLatestChart<LatestChartSettings>;
  onItemClick?: ($event: Event, item: LatestChartDataItem) => void;
}

@Component({
    selector: 'tb-latest-chart',
    templateUrl: './latest-chart.component.html',
    styleUrls: ['./latest-chart.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class LatestChartComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('chartContent', {static: false})
  chartContent: ElementRef<HTMLElement>;

  @ViewChild('chartShape', {static: false})
  chartShape: ElementRef<HTMLElement>;

  @ViewChild('chartLegend', {static: false})
  chartLegend: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  @Input()
  callbacks: LatestChartComponentCallbacks;

  @Input()
  settings: LatestChartWidgetSettings;

  showLegend: boolean;
  legendClass: string;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  get legendItems(): LatestChartLegendItem[] {
    return this.latestChart ? this.latestChart.getLegendItems() : [];
  }

  legendLabelStyle: ComponentStyle;
  legendValueStyle: ComponentStyle;
  disabledLegendLabelStyle: ComponentStyle;
  disabledLegendValueStyle: ComponentStyle;

  private shapeResize$: ResizeObserver;
  private legendHorizontal: boolean;

  private latestChart: TbLatestChart<LatestChartSettings>;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private renderer: Renderer2,
              private translate: TranslateService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.showLegend = this.settings.showLegend;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    if (this.showLegend) {
      this.legendClass = `legend-${this.settings.legendPosition}`;
      this.legendHorizontal = [LegendPosition.left, LegendPosition.right].includes(this.settings.legendPosition);
      this.legendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.disabledLegendLabelStyle = textStyle(this.settings.legendLabelFont);
      this.legendLabelStyle.color = this.settings.legendLabelColor;
      this.legendValueStyle = textStyle(this.settings.legendValueFont);
      this.disabledLegendValueStyle = textStyle(this.settings.legendValueFont);
      this.legendValueStyle.color = this.settings.legendValueColor;
    }
  }

  ngAfterViewInit() {
    this.latestChart = this.callbacks.createChart(this.chartShape, this.renderer);
    this.latestChart.onItemClick(this.callbacks.onItemClick);
    this.shapeResize$ = new ResizeObserver(() => {
      this.onResize();
    });
    this.shapeResize$.observe(this.chartContent.nativeElement);
    this.onResize();
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.latestChart) {
      this.latestChart.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public onDataUpdated() {
    if (this.latestChart) {
      this.latestChart.update();
    }
    if (this.showLegend) {
      this.cd.detectChanges();
      if (this.legendHorizontal) {
        setTimeout(() => {
          this.onResize();
        });
      }
    }
  }

  public onLegendItemEnter(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.keyEnter(item.dataKey);
    }
  }

  public onLegendItemLeave(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.keyLeave(item.dataKey);
    }
  }

  public toggleLegendItem(item: LatestChartLegendItem) {
    if (!item.total && item.hasValue) {
      this.latestChart.toggleKey(item.dataKey);
    }
  }

  private onResize() {
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.chartShape.nativeElement, 'min-width', null);
    }
    const shapeWidth = this.chartShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.chartShape.nativeElement.getBoundingClientRect().height;
    const size = Math.min(shapeWidth, shapeHeight);
    if (this.legendHorizontal) {
      this.renderer.setStyle(this.chartShape.nativeElement, 'min-width', `${size}px`);
    }
  }

}
