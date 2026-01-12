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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatasourceData, FormattedData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  createLabelFromPattern,
  flatDataWithoutOverride,
  formattedDataFormDatasourceData,
  isNumber,
  isObject,
  parseTbFunction,
  safeExecuteTbFunction,
  unwrapModule
} from '@core/utils';
import { CompiledTbFunction, TbFunction } from '@shared/models/js-function.models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

interface QrCodeWidgetSettings {
  qrCodeTextPattern: string;
  useQrCodeTextFunction: boolean;
  qrCodeTextFunction: TbFunction;
}

type QrCodeTextFunction = (data: FormattedData[]) => string;

@Component({
  selector: 'tb-qrcode-widget',
  templateUrl: './qrcode-widget.component.html',
  styleUrls: []
})
export class QrCodeWidgetComponent extends PageComponent implements OnInit, AfterViewInit {

  settings: QrCodeWidgetSettings;
  qrCodeTextFunction: Observable<CompiledTbFunction<QrCodeTextFunction>>;

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
    this.qrCodeTextFunction = this.settings.useQrCodeTextFunction ? parseTbFunction(this.ctx.http, this.settings.qrCodeTextFunction, ['data']) : null;
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
    const data = formattedDataFormDatasourceData(initialData);
    const pattern = this.settings.useQrCodeTextFunction ?
      this.qrCodeTextFunction.pipe(map(qrCodeTextFunction => safeExecuteTbFunction(qrCodeTextFunction, [data]))) : this.settings.qrCodeTextPattern;
    if (typeof pattern === 'string') {
      this.updateQrCodeText(pattern, data);
    } else {
      pattern.subscribe((text) => {
        this.updateQrCodeText(text, data);
      });
    }
  }

  private updateQrCodeText(pattern: string, data: FormattedData[]): void {
    const allData: FormattedData = flatDataWithoutOverride(data);
    const newQrCodeText = createLabelFromPattern(pattern, allData);
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
        unwrapModule(QRCode).toCanvas(this.canvasRef.nativeElement, this.qrCodeText);
        this.canvasRef.nativeElement.style.width = 'auto';
        this.canvasRef.nativeElement.style.height = 'auto';
      });
    } else {
      this.scheduleUpdateCanvas = true;
    }
  }
}
