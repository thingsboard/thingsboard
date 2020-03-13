///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import {Component, ElementRef, Inject, Input, NgZone, OnDestroy, OnInit, ViewContainerRef} from "@angular/core";
import {PageComponent} from "@shared/components/page.component";
import {WidgetContext} from "@home/models/widget-component.models";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {Overlay} from "@angular/cdk/overlay";
import {UtilsService} from "@core/services/utils.service";
import {Datasource, DatasourceType} from "@shared/models/widget.models";
import {WINDOW} from '@core/services/window.service';
import {from, Observable} from "rxjs";

interface webCameraInputWidgetSettings {
  widgetTitle: string;
}

interface inputDeviceInfo {
  deviceId?: string;
  label: string;
}

@Component({
  selector: 'tb-web-camera-widget',
  templateUrl: './web-camera-input.component.html',
  styleUrls: ['./web-camera-input.component.scss']
})
export class WebCameraInputWidgetComponent extends PageComponent implements OnInit, OnDestroy {
  @Input()
  ctx: WidgetContext;

  private settings: webCameraInputWidgetSettings;
  private datasource: Datasource;
  private videoInput: inputDeviceInfo[];

  isEntityDetected: boolean = false;
  dataKeyDetected: boolean = false;
  isCameraSupport: boolean = false;
  isDeviceDetect: boolean = false;
  isShowCamera: boolean = false;
  isPreviewPhoto: boolean = false;
  toastTargetId = 'web-camera-input-widget' + this.utils.guid();
  previewPhoto: any;

  constructor(@Inject(WINDOW) private window: Window,
              protected store: Store<AppState>,
              private elementRef: ElementRef,
              private ngZone: NgZone,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              // private attributeService: AttributeService,
              // private translate: TranslateService
  ) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.webCameraInputWidget = this;
    this.settings = this.ctx.settings;
    this.datasource = this.ctx.datasources[0];

    if (this.settings.widgetTitle && this.settings.widgetTitle.length) {
      this.ctx.widgetTitle = this.utils.customTranslation(this.settings.widgetTitle, this.settings.widgetTitle);
    } else {
      this.ctx.widgetTitle = this.ctx.widgetConfig.title;
    }

    if (this.datasource.type === DatasourceType.entity) {
      if (this.datasource.entityType && this.datasource.entityId) {
        this.isEntityDetected = true;
      }
    }
    if (this.datasource.dataKeys.length) {
      this.dataKeyDetected = true;
    }
    if (WebCameraInputWidgetComponent.hasGetUserMedia()) {
      this.isCameraSupport = true;
      //   WebCameraInputWidgetComponent.getDevices().pipe(
      //     filter((device) => device.kind === 'videoinput')
      //   )
      //     .subscribe(() => {
      //       this.isDeviceDetect = !!this.videoInput.length;
      //     });
      // }
    }
  }

  ngOnDestroy(): void {

  }

  private static hasGetUserMedia(): boolean {
    return !!(window.navigator.mediaDevices && window.navigator.mediaDevices.getUserMedia);
  }

  private static getDevices(): Observable<MediaDeviceInfo[]> {
    return from(window.navigator.mediaDevices.enumerateDevices());
  }

  private gotDevices(deviceInfos: MediaDeviceInfo[]): inputDeviceInfo[] {
    let videoInput: inputDeviceInfo[] = [];
    for (const deviceInfo of deviceInfos) {
      let device: inputDeviceInfo = {
        deviceId: deviceInfo.deviceId,
        label: ""
      };
      if (deviceInfo.kind === 'videoinput') {
        device.label = deviceInfo.label || `Camera ${this.videoInput.length + 1}`;
        this.videoInput.push(device);
      }
    }
    return videoInput;
  }

  takePhoto() {

  }

  cancelPhoto() {

  }

  savePhoto() {

  }

  switchWebCamera() {

  }

  createPhoto() {

  }

  closeCamera() {

  }
}
