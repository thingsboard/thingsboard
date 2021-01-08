///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { guid } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyJs } from '@shared/models/beautify.models';

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
export class JsonContentComponent implements OnInit, ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @ViewChild('jsonEditor', {static: true})
  jsonEditorElmRef: ElementRef;

  private jsonEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `jsonContentEditor-${guid()}`;

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

  contentBody: string;

  contentValid: boolean;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService) {
  }

  ngOnInit(): void {
    const editorElement = this.jsonEditorElmRef.nativeElement;
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
    getAce().subscribe(
      (ace) => {
        this.jsonEditor = ace.edit(editorElement, editorOptions);
        this.jsonEditor.session.setUseWrapMode(true);
        this.jsonEditor.setValue(this.contentBody ? this.contentBody : '', -1);
        this.jsonEditor.setReadOnly(this.disabled || this.readonly);
        this.jsonEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupJsonErrors();
            this.updateView();
          }
        });
        this.jsonEditor.on('blur', () => {
          this.contentValid = !this.validateContent || this.doValidate(true);
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

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'contentType') {
          if (this.jsonEditor) {
            let mode = 'text';
            if (this.contentType) {
              mode = contentTypesMap.get(this.contentType).code;
            }
            this.jsonEditor.session.setMode(`ace/mode/${mode}`);
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
    this.disabled = isDisabled;
    if (this.jsonEditor) {
      this.jsonEditor.setReadOnly(this.disabled || this.readonly);
    }
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
      this.cleanupJsonErrors();
      this.contentValid = true;
      this.propagateChange(this.contentBody);
      this.contentValid = this.doValidate(true);
      this.propagateChange(this.contentBody);
    }
  }

  private doValidate(showErrorToast = false): boolean {
    try {
      if (this.validateContent && this.contentType === ContentType.JSON) {
        JSON.parse(this.contentBody);
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

  cleanupJsonErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
  }

  writeValue(value: string): void {
    this.contentBody = value;
    this.contentValid = true;
    if (this.jsonEditor) {
      this.ignoreChange = true;
      this.jsonEditor.setValue(this.contentBody ? this.contentBody : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.jsonEditor.getValue();
    if (this.contentBody !== editorValue) {
      this.contentBody = editorValue;
      this.contentValid = !this.validateOnChange || this.doValidate();
      this.propagateChange(this.contentBody);
    }
  }

  beautifyJSON() {
    beautifyJs(this.contentBody, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.jsonEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  minifyJSON() {
    const res = JSON.stringify(this.contentBody);
    this.jsonEditor.setValue(res ? res : '', -1);
    this.updateView();
  }

  onFullscreen() {
    if (this.jsonEditor) {
      setTimeout(() => {
        this.jsonEditor.resize();
      }, 0);
    }
  }

}
