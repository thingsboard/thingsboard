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

import {
  Component,
  ElementRef,
  Inject,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { Datasource, DatasourceData, DatasourceType } from '@shared/models/widget.models';
import { WINDOW } from '@core/services/window.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable } from 'rxjs';
import { isString } from '@core/utils';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

interface PhotoCameraInputWidgetSettings {
  widgetTitle: string;
  imageQuality: number;
  imageFormat: string;
  maxWidth: number;
  maxHeight: number;
}

// @dynamic
@Component({
  selector: 'tb-photo-camera-widget',
  templateUrl: './photo-camera-input.component.html',
  styleUrls: ['./photo-camera-input.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class PhotoCameraInputWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  constructor(@Inject(WINDOW) private window: Window,
              protected store: Store<AppState>,
              private elementRef: ElementRef,
              private ngZone: NgZone,
              private overlay: Overlay,
              private utils: UtilsService,
              private attributeService: AttributeService,
              private sanitizer: DomSanitizer
  ) {
    super(store);
  }

  public get videoElement() {
    return this.videoStreamRef.nativeElement;
  }

  public get canvasElement() {
    return this.canvasRef.nativeElement;
  }

  public get videoWidth() {
    const videoRatio = this.getVideoAspectRatio();
    return Math.min(this.width, this.height * videoRatio);
  }

  public get videoHeight() {
    const videoRatio = this.getVideoAspectRatio();
    return Math.min(this.height, this.width / videoRatio);
  }

  private static DEFAULT_IMAGE_TYPE = 'image/jpeg';
  private static DEFAULT_IMAGE_QUALITY = 0.92;

  @Input()
  ctx: WidgetContext;

  @ViewChild('videoStream', {static: false}) videoStreamRef: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  private videoInputsIndex = 0;
  private settings: PhotoCameraInputWidgetSettings;
  private datasource: Datasource;
  private width = 640;
  private height = 480;
  private availableVideoInputs: MediaDeviceInfo[];
  private mediaStream: MediaStream;

  isEntityDetected = false;
  dataKeyDetected = false;
  isProtocolHttps = false;
  isCameraSupport = false;
  isDeviceDetect = false;
  isShowCamera = false;
  isPreviewPhoto = false;
  isHavePermissionCamera = true;
  isLoading = false;
  singleDevice = true;
  updatePhoto = false;
  previewPhoto: SafeUrl;
  lastPhoto: SafeUrl;

  private static hasGetUserMedia(): boolean {
    return !!(window.navigator.mediaDevices && window.navigator.mediaDevices.getUserMedia);
  }

  private static getAvailableVideoInputs(): Promise<MediaDeviceInfo[]> {
    return new Promise((resolve, reject) => {
      navigator.mediaDevices.enumerateDevices()
        .then((devices: MediaDeviceInfo[]) => {
          resolve(devices.filter((device: MediaDeviceInfo) => device.kind === 'videoinput'));
        })
        .catch(err => {
          reject(err.message || err);
        });
    });
  }

  ngOnInit(): void {
    this.ctx.$scope.photoCameraInputWidget = this;
    this.isLoading = true;
    this.settings = this.ctx.settings;
    this.datasource = this.ctx.datasources[0];

    if (this.settings.widgetTitle && this.settings.widgetTitle.length) {
      this.ctx.widgetTitle = this.utils.customTranslation(this.settings.widgetTitle, this.settings.widgetTitle);
    } else {
      this.ctx.widgetTitle = this.ctx.widgetConfig.title;
    }

    this.width = this.settings.maxWidth ? this.settings.maxWidth : 640;
    this.height = this.settings.maxHeight ? this.settings.maxWidth : 480;

    if (this.datasource.type === DatasourceType.entity) {
      if (this.datasource.entityType && this.datasource.entityId) {
        this.isEntityDetected = true;
      }
    }
    if (this.datasource.dataKeys.length) {
      this.dataKeyDetected = true;
    }
    this.detectAvailableDevices();
  }

  ngOnDestroy(): void {
    this.stopMediaTracks();
  }

  private updateWidgetData(data: Array<DatasourceData>) {
    const keyData = data[0].data;
    if (keyData?.length && isString(keyData[0][1]) && keyData[0][1].startsWith('data:image/')) {
      this.lastPhoto = this.sanitizer.bypassSecurityTrustUrl(keyData[0][1]);
    }
  }

  public onDataUpdated() {
    this.updateWidgetData(this.ctx.defaultSubscription.data);
    this.ctx.detectChanges();
  }


  private detectAvailableDevices(): void {
    if (this.window.location.protocol === 'https:' || this.window.location.hostname === 'localhost') {
      this.isProtocolHttps = true;

      if (PhotoCameraInputWidgetComponent.hasGetUserMedia()) {
        this.isCameraSupport = true;
        PhotoCameraInputWidgetComponent.getAvailableVideoInputs().then((devices) => {
            this.isLoading = false;
            this.isDeviceDetect = !!devices.length;
            this.singleDevice = devices.length < 2;
            this.availableVideoInputs = devices;
            this.ctx.detectChanges();
          }, () => {
            this.isLoading = false;
            this.availableVideoInputs = [];
          }
        );
      } else {
        this.isLoading = false;
      }
    } else {
      this.isLoading = false;
    }
  }

  private getVideoAspectRatio(): number {
    if (this.videoElement.videoWidth && this.videoElement.videoWidth > 0 &&
      this.videoElement.videoHeight && this.videoElement.videoHeight > 0) {
      return this.videoElement.videoWidth / this.videoElement.videoHeight;
    }
    return this.width / this.height;
  }

  private stopMediaTracks() {
    if (this.mediaStream && this.mediaStream.getTracks) {
      this.mediaStream.getTracks()
        .forEach((track: MediaStreamTrack) => track.stop());
    }
  }

  takePhoto() {
    this.inititedVideoStream(this.availableVideoInputs[this.videoInputsIndex].deviceId, true);
  }

  closeCamera() {
    this.stopMediaTracks();
    this.videoElement.srcObject = null;
    this.isShowCamera = false;
  }

  cancelPhoto() {
    this.isPreviewPhoto = false;
    this.previewPhoto = '';
  }

  savePhoto() {
    this.updatePhoto = true;
    let task: Observable<any>;
    const entityId: EntityId = {
      entityType: this.datasource.entityType,
      id: this.datasource.entityId
    };
    const saveData = [{
      key: this.datasource.dataKeys[0].name,
      value: this.previewPhoto
    }];
    if (this.datasource.dataKeys[0].type === DataKeyType.attribute) {
      task = this.attributeService.saveEntityAttributes(entityId, AttributeScope.SERVER_SCOPE, saveData);
    } else if (this.datasource.dataKeys[0].type === DataKeyType.timeseries) {
      task = this.attributeService.saveEntityTimeseries(entityId, 'scope', saveData);
    }
    task.subscribe(() => {
      this.isPreviewPhoto = false;
      this.updatePhoto = false;
      this.closeCamera();
    }, () => {
      this.updatePhoto = false;
    });
  }

  switchWebCamera() {
    this.videoInputsIndex = (this.videoInputsIndex + 1) % this.availableVideoInputs.length;
    this.stopMediaTracks();
    this.inititedVideoStream(this.availableVideoInputs[this.videoInputsIndex].deviceId);
  }

  createPhoto() {
    this.canvasElement.width = this.videoWidth;
    this.canvasElement.height = this.videoHeight;
    this.canvasElement.getContext('2d').drawImage(this.videoElement, 0, 0, this.videoWidth, this.videoHeight);

    const mimeType: string = this.settings.imageFormat ? this.settings.imageFormat : PhotoCameraInputWidgetComponent.DEFAULT_IMAGE_TYPE;
    const quality: number = this.settings.imageQuality ? this.settings.imageQuality : PhotoCameraInputWidgetComponent.DEFAULT_IMAGE_QUALITY;
    this.previewPhoto = this.canvasElement.toDataURL(mimeType, quality);
    this.isPreviewPhoto = true;
  }

  private inititedVideoStream(deviceId?: string, init = false) {
    if (window.navigator.mediaDevices && window.navigator.mediaDevices.getUserMedia) {
      const videoTrackConstraints = {
        video: {deviceId: deviceId !== '' ? {exact: deviceId} : undefined}
      };

      window.navigator.mediaDevices.getUserMedia(videoTrackConstraints).then((stream: MediaStream) => {
        if (init) {
          this.isShowCamera = true;
          if (this.availableVideoInputs.find((device) => device.deviceId === '')) {
            PhotoCameraInputWidgetComponent.getAvailableVideoInputs().then((devices) => {
              this.singleDevice = devices.length < 2;
              this.availableVideoInputs = devices;
              this.ctx.detectChanges();
            });
          }
        }
        this.mediaStream = stream;
        this.videoElement.srcObject = stream;
        this.ctx.detectChanges();
      }, () => {
        this.isHavePermissionCamera = false;
      });
    }
  }

  get textMessage() {
    if (this.isLoading) {
      return '';
    }
    if (!this.isProtocolHttps) {
      return 'widgets.input-widgets.enable-https-use-widget';
    }
    if (!this.isCameraSupport) {
      return 'widgets.input-widgets.no-support-web-camera';
    }
    if (!this.isEntityDetected) {
      return 'widgets.input-widgets.no-entity-selected';
    }
    if (!this.dataKeyDetected) {
      return 'widgets.input-widgets.no-datakey-selected';
    }
    if (!this.isDeviceDetect) {
      return 'widgets.input-widgets.no-found-your-camera';
    }
    if (!this.isHavePermissionCamera) {
      return 'widgets.input-widgets.no-permission-camera';
    }
    return null;
  }
}
