///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  OnDestroy,
  OnInit,
  Renderer2,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { BasicActionWidgetComponent } from '@home/components/widget/lib/action/action-widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { SVG, Svg } from '@svgdotjs/svg.js';
import { HttpClient } from '@angular/common/http';
import { ScadaObject, ScadaObjectSettings } from '@home/components/widget/lib/scada/scada.models';
import { ResizeObserver } from '@juggle/resize-observer';
import { ScadaTestWidgetSettings } from '@home/components/widget/lib/scada/scada-test-widget.models';

@Component({
  selector: 'tb-scada-test-widget',
  templateUrl: './scada-test-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './scada-test-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaTestWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('scadaShape', {static: false})
  scadaShape: ElementRef<HTMLElement>;

  private settings: ScadaTestWidgetSettings;

  private autoScale = true;
  private scadaObject: ScadaObject;

  private shapeResize$: ResizeObserver;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private renderer: Renderer2,
              protected cd: ChangeDetectorRef,
              private http: HttpClient) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...this.ctx.settings};
    this.scadaObject = new ScadaObject(this.ctx, '/assets/widget/scada/drawing.svg', this.settings.scadaObject);
    this.scadaObject.init().subscribe();
  }

  ngAfterViewInit(): void {
    this.scadaObject.addTo(this.scadaShape.nativeElement);
    if (this.autoScale) {
      this.shapeResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.shapeResize$.observe(this.scadaShape.nativeElement);
      this.onResize();
    }
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.scadaObject) {
      this.scadaObject.destroy();
    }
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
  }

  private onResize() {
    const shapeWidth = this.scadaShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.scadaShape.nativeElement.getBoundingClientRect().height;
    this.scadaObject.setSize(shapeWidth, shapeHeight);
  }
}
