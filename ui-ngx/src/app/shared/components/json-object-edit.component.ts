///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormControl,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { Ace } from 'ace-builds';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { guid, isDefinedAndNotNull, isObject, isUndefined } from '@core/utils';
import { getAce } from '@shared/models/ace/ace.models';
import { coerceBoolean } from '@shared/decorators/coercion';

export const jsonRequired = (control: AbstractControl): ValidationErrors | null => !control.value ? {required: true} : null;

@Component({
  selector: 'tb-json-object-edit',
  templateUrl: './json-object-edit.component.html',
  styleUrls: ['./json-object-edit.component.scss'],
  encapsulation: ViewEncapsulation.None,
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
export class JsonObjectEditComponent implements OnInit, ControlValueAccessor, Validator, OnDestroy {

  @HostBinding('style.position') position = 'relative';

  @ViewChild('jsonEditor', {static: true})
  jsonEditorElmRef: ElementRef;

  private jsonEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;

  toastTargetId = `jsonObjectEditor-${guid()}`;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: { [klass: string]: any };

  @Input() sort: (key: string, value: any) => any;

  @coerceBoolean()
  @Input()
  jsonRequired: boolean;

  @coerceBoolean()
  @Input()
  readonly: boolean;

  fullscreen = false;

  modelValue: any;

  contentValue: string;

  objectValid: boolean;

  validationError: string;

  errorShowed = false;

  ignoreChange = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const editorElement = this.jsonEditorElmRef.nativeElement;
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
    getAce().subscribe(
      (ace) => {
        this.jsonEditor = ace.edit(editorElement, editorOptions);
        this.jsonEditor.session.setUseWrapMode(false);
        this.jsonEditor.setValue(this.contentValue ? this.contentValue : '', -1);
        this.jsonEditor.setReadOnly(this.disabled || this.readonly);
        this.jsonEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupJsonErrors();
            this.updateView();
          }
        });
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.jsonEditor) {
      this.jsonEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.jsonEditor.resize();
      this.jsonEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.jsonEditor) {
      this.jsonEditor.setReadOnly(this.disabled || this.readonly);
    }
  }

  public validate(c: UntypedFormControl) {
    return (this.objectValid) ? null : {
      jsonParseError: {
        valid: false,
      },
    };
  }

  validateOnSubmit(): void {
    if (!this.disabled && !this.readonly) {
      this.cleanupJsonErrors();
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

  cleanupJsonErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
  }

  beautifyJSON() {
    if (this.jsonEditor && this.objectValid) {
      const res = JSON.stringify(this.modelValue, null, 2);
      this.jsonEditor.setValue(res ? res : '', -1);
      this.updateView();
    }
  }

  minifyJSON() {
    if (this.jsonEditor && this.objectValid) {
      const res = JSON.stringify(this.modelValue);
      this.jsonEditor.setValue(res ? res : '', -1);
      this.updateView();
    }
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.contentValue = '';
    this.objectValid = false;
    try {
      if (isDefinedAndNotNull(this.modelValue)) {
        this.contentValue = JSON.stringify(this.modelValue, isUndefined(this.sort) ? undefined :
          (key, objectValue) => this.sort(key, objectValue), 2);
        this.objectValid = true;
      } else {
        this.objectValid = !this.jsonRequired;
        this.validationError = 'Json object is required.';
      }
    } catch (e) {
      //
    }
    if (this.jsonEditor) {
      this.ignoreChange = true;
      this.jsonEditor.setValue(this.contentValue ? this.contentValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.jsonEditor.getValue();
    if (this.contentValue !== editorValue) {
      this.contentValue = editorValue;
      let data = null;
      this.objectValid = false;
      if (this.contentValue && this.contentValue.length > 0) {
        try {
          data = JSON.parse(this.contentValue);
          if (!isObject(data)) {
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
        this.objectValid = !this.jsonRequired;
        this.validationError = this.jsonRequired ? 'Json object is required.' : '';
      }
      this.modelValue = data;
      this.propagateChange(data);
      this.cd.markForCheck();
    }
  }

  onFullscreen() {
    if (this.jsonEditor) {
      setTimeout(() => {
        this.jsonEditor.resize();
      }, 0);
    }
  }

}
