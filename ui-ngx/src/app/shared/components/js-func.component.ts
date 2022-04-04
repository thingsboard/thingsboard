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
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { Range } from '@shared/models/ace/ace.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { isUndefined } from '@app/core/utils';
import { TranslateService } from '@ngx-translate/core';
import { RafService } from '@core/services/raf.service';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { beautifyJs } from '@shared/models/beautify.models';
import { EditorAce } from '@shared/models/ace/editor-ace.models';

@Component({
  selector: 'tb-js-func',
  templateUrl: './js-func.component.html',
  styleUrls: ['./js-func.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsFuncComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsFuncComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class JsFuncComponent  extends EditorAce implements OnInit, ControlValueAccessor, Validator {

  @ViewChild('javascriptEditor', {static: true})
  editorElementRef: ElementRef;

  @Input() functionName: string;

  @Input() functionArgs: Array<string>;

  @Input() validationArgs: Array<any>;

  @Input() resultType: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorCompleter: TbEditorCompleter;

  @Input() globalVariables: Array<string>;

  @Input() disableUndefinedCheck = false;

  @Input() helpId: string;

  private noValidateValue: boolean;
  get noValidate(): boolean {
    return this.noValidateValue;
  }
  @Input()
  set noValidate(value: boolean) {
    this.noValidateValue = coerceBooleanProperty(value);
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  functionArgsString = '';

  fullscreen = false;

  functionValid = true;

  validationError: string;

  errorShowed = false;

  errorMarkers: number[] = [];
  errorAnnotationId = -1;

  readonly = false;

  private propagateChange = null;
  private hasErrors = false;

  constructor(public elementRef: ElementRef,
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              protected raf: RafService) {
    super(raf);
  }

  ngOnInit(): void {
    if (!this.resultType || this.resultType.length === 0) {
      this.resultType = 'nocheck';
    }
    if (this.functionArgs) {
      this.functionArgs.forEach((functionArg) => {
        if (this.functionArgsString.length > 0) {
          this.functionArgsString += ', ';
        }
        this.functionArgsString += functionArg;
      });
    }
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/javascript',
      showGutter: true,
      showPrintMargin: true,
      readOnly: this.disabled
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    super.initEditor('javascript', 'textmate', editorOptions, true, this.modelValue);
  }

  setAdvancedEditorSetting() {
    if (!this.disableUndefinedCheck) {
      // @ts-ignore
      this.editor.session.on('changeAnnotation', () => {
        const annotations = this.editor.session.getAnnotations();
        annotations.filter(annotation => annotation.text.includes('is not defined')).forEach(annotation => {
          annotation.type = 'error';
        });
        this.editor.renderer.setAnnotations(annotations);
        const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
        if (this.hasErrors !== hasErrors) {
          this.hasErrors = hasErrors;
          this.propagateChange(this.modelValue);
        }
      });
    }
    // @ts-ignore
    if (!!this.editor.session.$worker) {
      const jsWorkerOptions = {
        undef: !this.disableUndefinedCheck,
        unused: true,
        globals: {}
      };
      if (!this.disableUndefinedCheck && this.functionArgs) {
        this.functionArgs.forEach(arg => {
          jsWorkerOptions.globals[arg] = false;
        });
      }
      if (!this.disableUndefinedCheck && this.globalVariables) {
        this.globalVariables.forEach(arg => {
          jsWorkerOptions.globals[arg] = false;
        });
      }
      // @ts-ignore
      this.editor.session.$worker.send('changeOptions', [jsWorkerOptions]);
    }
    if (this.editorCompleter) {
      this.editor.completers = [this.editorCompleter, ...(this.editor.completers || [])];
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
    return (this.functionValid && !this.hasErrors) ? null : {
      jsFunc: {
        valid: false,
      },
    };
  }

  beautifyJs() {
    beautifyJs(this.modelValue, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.editor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  validateOnSubmit(): void {
    if (!this.disabled) {
      this.cleanupErrors();
      this.functionValid = this.validateJsFunc();
      if (!this.functionValid) {
        this.propagateChange(this.modelValue);
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

  private validateJsFunc(): boolean {
    try {
      const toValidate = new Function(this.functionArgsString, this.modelValue);
      if (this.noValidate) {
        return true;
      }
      if (this.validationArgs) {
        let res: any;
        let validationError: any;
        for (const validationArg of this.validationArgs) {
          try {
            res = toValidate.apply(this, validationArg);
            validationError = null;
            break;
          } catch (e) {
            validationError = e;
          }
        }
        if (validationError) {
          throw validationError;
        }
        if (this.resultType !== 'nocheck') {
          if (this.resultType === 'any') {
            if (isUndefined(res)) {
              this.validationError = this.translate.instant('js-func.no-return-error');
              return false;
            }
          } else {
            const resType = typeof res;
            if (resType !== this.resultType) {
              this.validationError = this.translate.instant('js-func.return-type-mismatch', {type: this.resultType});
              return false;
            }
          }
        }
        return true;
      } else {
        return true;
      }
    } catch (e) {
      const details = this.utils.parseException(e);
      let errorInfo = 'Error:';
      if (details.name) {
        errorInfo += ' ' + details.name + ':';
      }
      if (details.message) {
        errorInfo += ' ' + details.message;
      }
      if (details.lineNumber) {
        errorInfo += '<br>Line ' + details.lineNumber;
        if (details.columnNumber) {
          errorInfo += ' column ' + details.columnNumber;
        }
        errorInfo += ' of script.';
      }
      this.validationError = errorInfo;
      if (details.lineNumber) {
        const line = details.lineNumber - 1;
        let column = 0;
        if (details.columnNumber) {
          column = details.columnNumber;
        }
        const errorMarkerId = this.editor.session.addMarker(new Range(line, 0, line, Infinity),
          'ace_active-line', 'screenLine');
        this.errorMarkers.push(errorMarkerId);
        const annotations = this.editor.session.getAnnotations();
        const errorAnnotation: Ace.Annotation = {
          row: line,
          column,
          text: details.message,
          type: 'error'
        };
        this.errorAnnotationId = annotations.push(errorAnnotation) - 1;
        this.editor.session.setAnnotations(annotations);
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
    this.errorMarkers.forEach((errorMarker) => {
      this.editor.session.removeMarker(errorMarker);
    });
    this.errorMarkers.length = 0;
    if (this.errorAnnotationId > -1) {
      const annotations = this.editor.session.getAnnotations();
      annotations.splice(this.errorAnnotationId, 1);
      this.editor.session.setAnnotations(annotations);
      this.errorAnnotationId = -1;
    }
  }

  writeValue(value: string): void {
    super.setValue(value);
  }

  updateView() {
    const editorValue = this.editor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.functionValid = true;
      this.propagateChange(this.modelValue);
    }
  }
}
