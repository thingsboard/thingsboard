///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  fillPattern, flatData,
  parseData,
  parseFunction,
  processPattern,
  safeExecute
} from '@home/components/widget/lib/maps/common-maps-utils';
import { FormattedData } from '@home/components/widget/lib/maps/map-models';
import { DatasourceData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { isNumber, isObject } from '@core/utils';

interface QrCodeWidgetSettings {
  qrCodeTextPattern: string;
  useQrCodeTextFunction: boolean;
  qrCodeTextFunction: string;
}

type QrCodeTextFunction = (data: FormattedData[]) => string;

@Component({
  selector: 'tb-qrcode-widget',
  templateUrl: './qrcode-widget.component.html',
  styleUrls: []
})
export class QrCodeWidgetComponent extends PageComponent implements OnInit, AfterViewInit {

  settings: QrCodeWidgetSettings;
  qrCodeTextFunction: QrCodeTextFunction;

  @Input()
  ctx: WidgetContext;

  qrCodeText: string;
  invalidQrCodeText = false;

  private viewInited: boolean;
  private scheduleUpdateCanvas: boolean;

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.qrCodeWidget = this;
    this.settings = this.ctx.settings;
    this.qrCodeTextFunction = this.settings.useQrCodeTextFunction ? parseFunction(this.settings.qrCodeTextFunction, ['data']) : null;
  }

  ngAfterViewInit(): void {
    this.viewInited = true;
    if (this.scheduleUpdateCanvas) {
      this.scheduleUpdateCanvas = false;
      this.updateCanvas();
    }
  }

  public onDataUpdated() {
    let initialData: DatasourceData[];
    let qrCodeText: string;
    if (this.ctx.data?.length) {
      initialData = this.ctx.data;
    } else if (this.ctx.datasources?.length) {
      initialData = [
        {
          datasource: this.ctx.datasources[0],
          dataKey: {
            type: DataKeyType.attribute,
            name: 'empty'
          },
          data: []
        }
      ];
    } else {
      initialData = [];
    }
    const data = parseData(initialData);
    const pattern = this.settings.useQrCodeTextFunction ?
      safeExecute(this.qrCodeTextFunction, [data]) : this.settings.qrCodeTextPattern;
    const allData = flatData(data);
    const replaceInfo = processPattern(pattern, allData);
    qrCodeText = fillPattern(pattern, replaceInfo, allData);
    this.updateQrCodeText(qrCodeText);
  }

  private updateQrCodeText(newQrCodeText: string): void {
    if (this.qrCodeText !== newQrCodeText) {
      this.qrCodeText = newQrCodeText;
      if (!(isObject(newQrCodeText) || isNumber(newQrCodeText))) {
        this.invalidQrCodeText = false;
        if (this.qrCodeText) {
          this.updateCanvas();
        }
      } else {
        this.invalidQrCodeText = true;
      }
      this.cd.detectChanges();
    }
  }

  private updateCanvas() {
    if (this.viewInited) {
      import('qrcode').then((QRCode) => {
        QRCode.toCanvas(this.canvasRef.nativeElement, this.qrCodeText);
        this.canvasRef.nativeElement.style.width = 'auto';
        this.canvasRef.nativeElement.style.height = 'auto';
      });
    } else {
      this.scheduleUpdateCanvas = true;
    }
  }

}
