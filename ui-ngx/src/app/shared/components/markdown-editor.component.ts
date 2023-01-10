///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { getAce } from '@shared/models/ace/ace.models';

@Component({
  selector: 'tb-markdown-editor',
  templateUrl: './markdown-editor.component.html',
  styleUrls: ['./markdown-editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkdownEditorComponent),
      multi: true
    }
  ]
})
export class MarkdownEditorComponent implements OnInit, ControlValueAccessor {

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() readonly: boolean;

  @ViewChild('markdownEditor', {static: true})
  markdownEditorElmRef: ElementRef;

  private markdownEditor: Ace.Editor;

  editorMode = true;

  fullscreen = false;

  markdownValue: string;
  renderValue: string;

  ignoreChange = false;

  private propagateChange = null;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  constructor() {
  }

  ngOnInit(): void {
    if (!this.readonly) {
      const editorElement = this.markdownEditorElmRef.nativeElement;
      let editorOptions: Partial<Ace.EditorOptions> = {
        mode: 'ace/mode/markdown',
        showGutter: true,
        showPrintMargin: false,
        readOnly: false
      };

      const advancedOptions = {
        enableSnippets: true,
        enableBasicAutocompletion: true,
        enableLiveAutocompletion: true
      };

      editorOptions = {...editorOptions, ...advancedOptions};

      getAce().subscribe(
        (ace) => {
          this.markdownEditor = ace.edit(editorElement, editorOptions);
          this.markdownEditor.session.setUseWrapMode(false);
          this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
          this.markdownEditor.on('change', () => {
            if (!this.ignoreChange) {
              this.updateView();
            }
          });
        }
      );

    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: string): void {
    this.editorMode = true;
    this.markdownValue = value;
    this.renderValue = this.markdownValue ? this.markdownValue : ' ';
    if (this.markdownEditor) {
      this.ignoreChange = true;
      this.markdownEditor.setValue(this.markdownValue ? this.markdownValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.markdownEditor.getValue();
    if (this.markdownValue !== editorValue) {
      this.markdownValue = editorValue;
      this.renderValue = this.markdownValue ? this.markdownValue : ' ';
      this.propagateChange(this.markdownValue);
    }
  }

  onFullscreen() {
    if (this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }

  toggleEditMode() {
    this.editorMode = !this.editorMode;
    if (this.editorMode && this.markdownEditor) {
      setTimeout(() => {
        this.markdownEditor.resize();
      }, 0);
    }
  }
}
