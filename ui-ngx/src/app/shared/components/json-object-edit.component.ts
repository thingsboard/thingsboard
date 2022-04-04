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
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { RafService } from '@core/services/raf.service';
import { isDefinedAndNotNull, isLiteralObject, isUndefined } from '@core/utils';
import { EditorAce } from '@shared/models/ace/editor-ace.models';

@Component({
  selector: 'tb-json-object-edit',
  templateUrl: './json-object-edit.component.html',
  styleUrls: ['./json-object-edit.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonObjectEditComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsonObjectEditComponent),
      multi: true,
    }
  ]
})
export class JsonObjectEditComponent extends EditorAce implements OnInit, ControlValueAccessor, Validator {

  @ViewChild('jsonEditor', {static: true})
  editorElementRef: ElementRef;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: { [klass: string]: any };

  @Input() sort: (key: string, value: any) => any;

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

  fullscreen = false;

  modelValue: string;

  objectValid: boolean;

  validationError: string;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              private store: Store<AppState>,
              protected raf: RafService) {
    super(raf);
  }

  ngOnInit(): void {
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/json',
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
    super.initEditor('json', 'textmate', editorOptions, false, this.modelValue);
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
    return (this.objectValid) ? null : {
      jsonParseError: {
        valid: false,
      },
    };
  }

  validateOnSubmit(): void {
    if (!this.disabled && !this.readonly) {
      this.cleanupErrors();
      if (!this.objectValid) {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.validationError,
            type: 'error',
            target: this.toastTargetId,
            verticalPosition: 'bottom',
            horizontalPosition: 'left'
          }));
        this.errorShowed = true;
      }
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

  beautifyJSON() {
    if (this.editor && this.objectValid) {
      const res = JSON.stringify(this.modelValue, null, 2);
      this.editor.setValue(res ? res : '', -1);
      this.updateView();
    }
  }

  minifyJSON() {
    if (this.editor && this.objectValid) {
      const res = JSON.stringify(this.modelValue);
      this.editor.setValue(res ? res : '', -1);
      this.updateView();
    }
  }

  writeValue(value: any): void {
    let content = '';
    this.objectValid = false;
    try {
      if (isDefinedAndNotNull(value)) {
        content = JSON.stringify(value, isUndefined(this.sort) ? undefined :
          (key, objectValue) => {
            return this.sort(key, objectValue);
          }, 2);
        this.objectValid = true;
      } else {
        this.objectValid = !this.required;
        this.validationError = 'Json object is required.';
      }
    } catch (e) {
      //
    }
    super.setValue(content);
  }

  updateView() {
    const editorValue = this.editor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      let data = null;
      this.objectValid = false;
      if (this.modelValue && this.modelValue.length > 0) {
        try {
          data = JSON.parse(this.modelValue);
          if (!isLiteralObject(data)) {
            throw new TypeError(`Value is not a valid JSON`);
          }
          this.objectValid = true;
          this.validationError = '';
        } catch (ex) {
          let errorInfo = 'Error:';
          if (ex.name) {
            errorInfo += ' ' + ex.name + ':';
          }
          if (ex.message) {
            errorInfo += ' ' + ex.message;
          }
          this.validationError = errorInfo;
        }
      } else {
        this.objectValid = !this.required;
        this.validationError = this.required ? 'Json object is required.' : '';
      }
      this.propagateChange(data);
    }
  }
}
