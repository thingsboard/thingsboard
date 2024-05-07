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
import { HttpClient } from '@angular/common/http';
import { IotSvgObject } from '@home/components/widget/lib/svg/iot-svg.models';
import { ResizeObserver } from '@juggle/resize-observer';
import {
  iotSvgWidgetDefaultSettings,
  IotSvgWidgetSettings
} from '@home/components/widget/lib/svg/iot-svg-widget.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-iot-svg-widget',
  templateUrl: './iot-svg-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './iot-svg-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class IotSvgWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('iotSvgShape', {static: false})
  iotSvgShape: ElementRef<HTMLElement>;

  private settings: IotSvgWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  iotSvgObject: IotSvgObject;

  private autoScale = true;

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
    this.settings = {...iotSvgWidgetDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    this.iotSvgObject = new IotSvgObject(this.ctx, this.settings.iotSvg, this.settings.iotSvgObject);
    this.iotSvgObject.onError((error) => {
      this.ctx.showErrorToast(error, 'bottom', 'center', this.ctx.toastTargetId, true);
    });
    this.iotSvgObject.init().subscribe();
  }

  ngAfterViewInit(): void {
    this.iotSvgObject.addTo(this.iotSvgShape.nativeElement);
    if (this.autoScale) {
      this.shapeResize$ = new ResizeObserver(() => {
        this.onResize();
      });
      this.shapeResize$.observe(this.iotSvgShape.nativeElement);
      this.onResize();
    }
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    if (this.shapeResize$) {
      this.shapeResize$.disconnect();
    }
    if (this.iotSvgObject) {
      this.iotSvgObject.destroy();
    }
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  private onResize() {
    const shapeWidth = this.iotSvgShape.nativeElement.getBoundingClientRect().width;
    const shapeHeight = this.iotSvgShape.nativeElement.getBoundingClientRect().height;
    this.iotSvgObject.setSize(shapeWidth, shapeHeight);
  }
}
