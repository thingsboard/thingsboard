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

import { Component, ElementRef, forwardRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { RafService } from '@core/services/raf.service';
import { beautifyJs } from '@shared/models/beautify.models';
import { EditorAce } from '@shared/models/ace/editor-ace.models';

@Component({
  selector: 'tb-json-content',
  templateUrl: './json-content.component.html',
  styleUrls: ['./json-content.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonContentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsonContentComponent),
      multi: true,
    }
  ]
})
export class JsonContentComponent extends EditorAce implements OnInit, ControlValueAccessor, Validator, OnChanges {

  @ViewChild('jsonEditor', {static: true})
  editorElementRef: ElementRef;

  @Input() label: string;

  @Input() contentType: ContentType;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  private validateContentValue: boolean;
  get validateContent(): boolean {
    return this.validateContentValue;
  }
  @Input()
  set validateContent(value: boolean) {
    this.validateContentValue = coerceBooleanProperty(value);
  }

  private validateOnChangeValue: boolean;
  get validateOnChange(): boolean {
    return this.validateOnChangeValue;
  }
  @Input()
  set validateOnChange(value: boolean) {
    this.validateOnChangeValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  contentValid: boolean;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              protected raf: RafService) {
    super(raf);
  }

  ngOnInit(): void {
    let mode = 'text';
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
    }
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${mode}`,
      showGutter: true,
      showPrintMargin: false,
      readOnly: this.disabled || this.readonly
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    super.initEditor(['json', 'text'], 'textmate', editorOptions, true, this.modelValue);
  }

  setAdvancedEditorSetting() {
    this.editor.on('blur', () => {
      this.contentValid = !this.validateContent || this.doValidate(true);
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'contentType') {
          if (this.editor) {
            let mode = 'text';
            if (this.contentType) {
              mode = contentTypesMap.get(this.contentType).code;
            }
            this.editor.session.setMode(`ace/mode/${mode}`);
          }
        }
      }
    }
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
    return (this.contentValid) ? null : {
      contentBody: {
        valid: false,
      },
    };
  }

  validateOnSubmit(): void {
    if (!this.disabled && !this.readonly) {
      this.cleanupErrors();
      this.contentValid = true;
      this.propagateChange(this.modelValue);
      this.contentValid = this.doValidate(true);
      this.propagateChange(this.modelValue);
    }
  }

  private doValidate(showErrorToast = false): boolean {
    try {
      if (this.validateContent && this.contentType === ContentType.JSON) {
        JSON.parse(this.modelValue);
      }
      return true;
    } catch (ex) {
      if (showErrorToast) {
        let errorInfo = 'Error:';
        if (ex.name) {
          errorInfo += ' ' + ex.name + ':';
        }
        if (ex.message) {
          errorInfo += ' ' + ex.message;
        }
        this.store.dispatch(new ActionNotificationShow(
          {
            message: errorInfo,
            type: 'error',
            target: this.toastTargetId,
            verticalPosition: 'bottom',
            horizontalPosition: 'left'
          }));
        this.errorShowed = true;
      }
      return false;
    }
  }

  cleanupErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
  }

  writeValue(value: string): void {
    this.contentValid = true;
    super.setValue(this.modelValue);
  }

  updateView() {
    const editorValue = this.editor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.contentValid = !this.validateOnChange || this.doValidate();
      this.propagateChange(this.modelValue);
    }
  }

  beautifyJSON() {
    beautifyJs(this.modelValue, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.editor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  minifyJSON() {
    const res = JSON.stringify(this.modelValue);
    this.editor.setValue(res ? res : '', -1);
    this.updateView();
  }
}
