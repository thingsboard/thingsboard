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
import { ScadaSymbolEditObject, ScadaSymbolEditObjectCallbacks } from '@home/pages/scada-symbol/scada-symbol-editor.models';

export interface ScadaSymbolEditorData {
  scadaSymbolContent: string;
}

@Component({
  selector: 'tb-scada-symbol-editor',
  templateUrl: './scada-symbol-editor.component.html',
  styleUrls: ['./scada-symbol-editor.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolEditorComponent implements OnInit, OnDestroy, AfterViewInit, OnChanges {

  @ViewChild('scadaSymbolShape', {static: false})
  scadaSymbolShape: ElementRef<HTMLElement>;

  @Input()
  data: ScadaSymbolEditorData;

  @Input()
  editObjectCallbacks: ScadaSymbolEditObjectCallbacks;

  @Input()
  readonly: boolean;

  scadaSymbolEditObject: ScadaSymbolEditObject;

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.scadaSymbolShape.nativeElement,
      this.viewContainerRef, this.editObjectCallbacks, this.readonly);
    if (this.data) {
      this.scadaSymbolEditObject.setContent(this.data.scadaSymbolContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          if (this.scadaSymbolEditObject) {
            setTimeout(() => {
              this.scadaSymbolEditObject.setContent(this.data.scadaSymbolContent);
            });
          }
        } else if (propName === 'readonly') {
          this.scadaSymbolEditObject.setReadOnly(this.readonly);
        }
      }
    }
  }

  ngOnDestroy() {
    this.scadaSymbolEditObject.destroy();
  }

  getContent(): string {
    return this.scadaSymbolEditObject?.getContent();
  }
}

