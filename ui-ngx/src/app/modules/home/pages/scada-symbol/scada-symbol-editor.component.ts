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
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { Subject } from 'rxjs';
import { ScadaSymbolEditObject } from '@home/pages/scada-symbol/scada-symbol.models';

@Component({
  selector: 'tb-scada-symbol-editor',
  templateUrl: './scada-symbol-editor.component.html',
  styleUrls: ['./scada-symbol-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolEditorComponent implements OnInit, OnDestroy, AfterViewInit, OnChanges {

  @ViewChild('iotSvgShape', {static: false})
  iotSvgShape: ElementRef<HTMLElement>;

  @Input()
  svgContent: string;

  scadaSymbolEditObject: ScadaSymbolEditObject;

  private destroy$ = new Subject<void>();

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.iotSvgShape.nativeElement,
      this.viewContainerRef);
    if (this.svgContent) {
      this.scadaSymbolEditObject.setContent(this.svgContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'svgContent') {
          if (this.scadaSymbolEditObject) {
            this.scadaSymbolEditObject.setContent(this.svgContent);
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.scadaSymbolEditObject.destroy();
  }

}

