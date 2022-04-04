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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { Ace } from 'ace-builds';
import { RafService } from '@core/services/raf.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { AceMode } from '@shared/models/ace/ace.models';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { beautifyCss, beautifyJs } from '@shared/models/beautify.models';
import { Observable, of } from 'rxjs';
import { EditorAce } from '@shared/models/ace/editor-ace.models';

@Component({
  selector: 'tb-editor',
  templateUrl: './editor.component.html',
  styleUrls: ['./editor.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EditorComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EditorComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class EditorComponent extends EditorAce implements OnInit, ControlValueAccessor, Validator {

  @ViewChild('editor', {static: true}) editorElementRef: ElementRef;

  @Input() label: string;

  @Input() mode: AceMode;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() placeholder: string;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  private showPrintMarginValue = false;
  get showPrintMargin(): boolean {
    return this.showPrintMarginValue;
  }
  @Input()
  set showPrintMargin(value: boolean) {
    this.showPrintMarginValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  modelValue: string;

  hasErrors = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected raf: RafService) {
    super(raf);
  }

  ngOnInit(): void {
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${this.mode}`,
      showGutter: true,
      showPrintMargin: this.showPrintMargin,
      readOnly: this.disabled || this.readonly,
      placeholder: this.placeholder
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    super.initEditor(this.mode, 'textmate', editorOptions, true, this.modelValue);
  }

  setAdvancedEditorSetting() {
    // @ts-ignore
    this.editor.session.on('changeAnnotation', () => {
      const annotations = this.editor.session.getAnnotations();
      const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
      if (this.hasErrors !== hasErrors) {
        this.hasErrors = hasErrors;
        this.propagateChange(this.modelValue);
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    super.setDisabled(isDisabled);
  }

  public validate(c: FormControl) {
    return (!this.hasErrors) ? null : {
      editor: false,
    };
  }

  beautify() {
    let beautify$: Observable<any>;
    switch (this.mode) {
      case 'css':
        beautify$ = beautifyCss(this.modelValue, {indent_size: 4});
        break;
      case 'protobuf':
        beautify$ = beautifyJs(this.modelValue, {indent_size: 4, wrap_line_length: 60});
        break;
      default:
        beautify$ = of(this.modelValue);
    }
    beautify$.subscribe(
      (res) => {
        if (this.modelValue !== res) {
          this.editor.setValue(res ? res : '', -1);
          this.updateView();
        }
      }
    );
  }

  writeValue(value: string): void {
    super.setValue(value);
  }

  updateView() {
    const editorValue = this.editor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.propagateChange(this.modelValue);
    }
  }
}
