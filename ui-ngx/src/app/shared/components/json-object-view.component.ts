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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, Renderer2, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { isDefinedAndNotNull, isUndefined } from '@core/utils';
import { getAce, updateEditorSize } from '@shared/models/ace/ace.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-json-object-view',
    templateUrl: './json-object-view.component.html',
    styleUrls: ['./json-object-view.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => JsonObjectViewComponent),
            multi: true
        }
    ],
    standalone: false
})
export class JsonObjectViewComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @ViewChild('jsonViewer', {static: true})
  jsonViewerElmRef: ElementRef;

  private jsonViewer: Ace.Editor;
  private viewerElement: Ace.Editor;
  private propagateChange = null;
  private modelValue: any;
  private contentValue: string;

  @Input() label: string;

  @Input() fillHeight: boolean;

  @Input() editorStyle: { [klass: string]: any };

  @Input() sort: (key: string, value: any) => any;

  @Input()
  @coerceBoolean()
  autoWidth: boolean

  @Input()
  @coerceBoolean()
  autoHeight: boolean

  constructor(private renderer: Renderer2) {
  }

  ngOnInit(): void {
    this.viewerElement = this.jsonViewerElmRef.nativeElement;
    const editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/java',
      theme: 'ace/theme/github',
      showGutter: false,
      showPrintMargin: false,
      readOnly: true,
      enableSnippets: false,
      enableBasicAutocompletion: false,
      enableLiveAutocompletion: false
    };

    getAce().subscribe(
      (ace) => {
        this.jsonViewer = ace.edit(this.viewerElement, editorOptions);
        this.jsonViewer.session.setUseWrapMode(false);
        this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
        if (this.contentValue && (this.autoWidth || this.autoHeight)) {
          updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer, this.renderer, {
            ignoreHeight: !this.autoHeight,
            ignoreWidth: !this.autoWidth
          });
        }
      }
    );
  }

  ngOnDestroy(): void {
    this.jsonViewer?.destroy();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.contentValue = '';
    try {
      if (isDefinedAndNotNull(this.modelValue)) {
        this.contentValue = JSON.stringify(this.modelValue, isUndefined(this.sort) ? undefined :
          (key, objectValue) => {
            return this.sort(key, objectValue);
          }, 2);
      }
    } catch (e) {
      console.error(e);
    }
    if (this.jsonViewer) {
      this.jsonViewer.setValue(this.contentValue ? this.contentValue : '', -1);
      if (this.contentValue && (this.autoWidth || this.autoHeight)) {
        updateEditorSize(this.viewerElement, this.contentValue, this.jsonViewer, this.renderer, {
          ignoreHeight: !this.autoHeight,
          ignoreWidth: !this.autoWidth
        });
      }
    }
  }

}
