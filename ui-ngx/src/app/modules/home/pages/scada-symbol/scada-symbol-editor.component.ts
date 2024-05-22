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
  ElementRef, EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit, Output,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ScadaSymbolEditObject } from '@home/pages/scada-symbol/scada-symbol.models';

export interface ScadaSymbolEditorData {
  svgContent: string;
}

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
  data: ScadaSymbolEditorData;

  @Output()
  tags = new EventEmitter<string[]>();

  scadaSymbolEditObject: ScadaSymbolEditObject;

  constructor(private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.iotSvgShape.nativeElement,
      this.viewContainerRef);
    this.scadaSymbolEditObject.tagsUpdated.subscribe(tags => this.tags.emit(tags));
    if (this.data) {
      this.scadaSymbolEditObject.setContent(this.data.svgContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          if (this.scadaSymbolEditObject) {
            setTimeout(() => {
              this.scadaSymbolEditObject.setContent(this.data.svgContent);
            });
          }
        }
      }
    }
  }

  ngOnDestroy() {
    this.scadaSymbolEditObject.destroy();
  }

  get dirty(): boolean {
    return this.scadaSymbolEditObject?.dirty;
  }

  getContent(): string {
    return this.scadaSymbolEditObject?.getContent();
  }

}

