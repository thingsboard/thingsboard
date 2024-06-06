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
  AfterViewInit, ChangeDetectorRef,
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
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';

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

  @ViewChild('tooltipsContainer', {static: false})
  tooltipsContainer: ElementRef<HTMLElement>;

  @ViewChild('tooltipsContainerComponent', {static: true})
  tooltipsContainerComponent: TbAnchorComponent;

  @Input()
  data: ScadaSymbolEditorData;

  @Input()
  editObjectCallbacks: ScadaSymbolEditObjectCallbacks;

  @Input()
  readonly: boolean;

  scadaSymbolEditObject: ScadaSymbolEditObject;

  zoomInDisabled = false;
  zoomOutDisabled = false;

  constructor(private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
    this.editObjectCallbacks.onZoom = () => {
      this.updateZoomButtonsState();
    };
    this.scadaSymbolEditObject = new ScadaSymbolEditObject(this.scadaSymbolShape.nativeElement,
      this.tooltipsContainer.nativeElement,
      this.tooltipsContainerComponent.viewContainerRef, this.editObjectCallbacks, this.readonly);
    if (this.data) {
      this.updateContent(this.data.scadaSymbolContent);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'data') {
          if (this.scadaSymbolEditObject) {
            setTimeout(() => {
              this.updateContent(this.data.scadaSymbolContent);
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

  zoomIn() {
    this.scadaSymbolEditObject.zoomIn();
  }

  zoomOut() {
    this.scadaSymbolEditObject.zoomOut();
  }

  private updateContent(content: string) {
    this.scadaSymbolEditObject.setContent(content);
    setTimeout(() => {
      this.updateZoomButtonsState();
    });
  }

  private updateZoomButtonsState() {
    this.zoomInDisabled = this.scadaSymbolEditObject.zoomInDisabled();
    this.zoomOutDisabled = this.scadaSymbolEditObject.zoomOutDisabled();
    this.cd.markForCheck();
  }
}

