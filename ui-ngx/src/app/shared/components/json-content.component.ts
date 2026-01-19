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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, UntypedFormControl, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { guid } from '@core/utils';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyJs } from '@shared/models/beautify.models';
import { coerceBoolean } from '@shared/decorators/coercion';

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
  ],
  encapsulation: ViewEncapsulation.None
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

  @Input() tbPlaceholder: string;

  @Input()
  @coerceBoolean()
  hideToolbar = false;

  @Input()
  @coerceBoolean()
  readonly: boolean;

  @Input()
  @coerceBoolean()
  validateContent: boolean;

  @Input()
  @coerceBoolean()
  validateOnChange: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  fullscreen = false;

  contentBody: string;

  contentValid: boolean;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
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
        if (this.validateContent) {
          this.jsonEditor.on('blur', () => {
            this.contentValid = this.doValidate(true);
            this.cd.markForCheck();
          });
        }

        if (this.tbPlaceholder && this.tbPlaceholder.length) {
          this.createPlaceholder();
        }
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  private createPlaceholder() {
    this.jsonEditor.on('input', this.updateEditorPlaceholder.bind(this));
    setTimeout(this.updateEditorPlaceholder.bind(this), 100);
  }

  private updateEditorPlaceholder() {
    const shouldShow = !this.jsonEditor.session.getValue().length;
    let node: HTMLElement = (this.jsonEditor.renderer as any).emptyMessageNode;
    if (!shouldShow && node) {
      this.jsonEditor.renderer.getMouseEventTarget().removeChild(node);
      (this.jsonEditor.renderer as any).emptyMessageNode = null;
    } else if (shouldShow && !node) {
      const placeholderElement = $('<textarea></textarea>');
      placeholderElement.text(this.tbPlaceholder);
      placeholderElement.addClass('ace_invisible ace_emptyMessage');
      placeholderElement.css({
        padding: '0 9px',
        width: '100%',
        border: 'none',
        textWrap: 'nowrap',
        whiteSpace: 'pre',
        overflow: 'hidden',
        resize: 'none',
        fontSize: '15px'
      });
      const rows = this.tbPlaceholder.split('\n').length;
      placeholderElement.attr('rows', rows);
      node = placeholderElement[0];
      (this.jsonEditor.renderer as any).emptyMessageNode = node;
      this.jsonEditor.renderer.getMouseEventTarget().appendChild(node);
    }
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

  public validate(c: UntypedFormControl) {
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
      this.cd.markForCheck();
    }
  }

  private doValidate(showErrorToast = false): boolean {
    try {
      if (this.contentType === ContentType.JSON) {
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
      this.cd.markForCheck();
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
