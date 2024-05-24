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
  Input,
  OnDestroy,
  OnInit,
  Renderer2,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { IotSvgObject, IotSvgObjectCallbacks } from '@home/components/widget/lib/svg/iot-svg.models';
import {
  iotSvgWidgetDefaultSettings,
  IotSvgWidgetSettings
} from '@home/components/widget/lib/svg/iot-svg-widget.models';
import { Observable, of } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { ImageService } from '@core/http/image.service';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-iot-svg-widget',
  templateUrl: './iot-svg-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './iot-svg-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class IotSvgWidgetComponent implements OnInit, AfterViewInit, OnDestroy, IotSvgObjectCallbacks {

  @ViewChild('iotSvgShape', {static: false})
  iotSvgShape: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  private settings: IotSvgWidgetSettings;
  private svgContent$: Observable<string>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  iotSvgObject: IotSvgObject;

  constructor(public widgetComponent: WidgetComponent,
              protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private imageService: ImageService,
              protected cd: ChangeDetectorRef,
              private http: HttpClient) {
  }

  ngOnInit(): void {
    this.ctx.$scope.actionWidget = this;
    this.settings = mergeDeep({} as IotSvgWidgetSettings, iotSvgWidgetDefaultSettings, this.ctx.settings || {});

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    if (this.settings.iotSvgContent) {
      this.svgContent$ = of(this.settings.iotSvgContent);
    } else if (this.settings.iotSvgUrl) {
      this.svgContent$ = this.imageService.getImageString(this.settings.iotSvgUrl);
    } else {
      this.svgContent$ = this.http.get(this.settings.iotSvg, {responseType: 'text'});
    }
  }

  ngAfterViewInit(): void {
    this.svgContent$.subscribe((content) => {
      this.initObject(this.iotSvgShape.nativeElement, content);
    });
  }

  ngOnDestroy() {
    if (this.iotSvgObject) {
      this.iotSvgObject.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  onSvgObjectError(error: string) {
    this.ctx.showErrorToast(error, 'bottom', 'center', this.ctx.toastTargetId, true);
  }

  onSvgObjectMessage(message: string) {
    this.ctx.showSuccessToast(message, 3000, 'bottom', 'center', this.ctx.toastTargetId, true);
  }

  private initObject(rootElement: HTMLElement,
                     svgContent: string) {
    const simulated = this.ctx.utilsService.widgetEditMode ||
      this.ctx.isPreview || (isDefinedAndNotNull(this.settings.simulated) ? this.settings.simulated : false);
    this.iotSvgObject = new IotSvgObject(rootElement, this.ctx, svgContent, this.settings.iotSvgObject, this, simulated);
  }

}
