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

import {
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Ace } from 'ace-builds';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { ResizeObserver } from '@juggle/resize-observer';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AceMode, getAce } from '@shared/models/ace/ace.models';
import { ControlValueAccessor, FormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { beautifyCss, beautifyJs } from '@shared/models/beautify.models';
import { Observable, of } from 'rxjs';

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
export class EditorComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('editor', {static: true}) editorElmRef: ElementRef;

  private editor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  @Input() label: string;

  @Input() mode: AceMode;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() placeholder: string;

  @Input() functionName: string;

  @Input() functionArgs: Array<string>;

  @Input() validationArgs: Array<any>;

  @Input() resultType: string;

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
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              private raf: RafService) {
  }

  ngOnInit(): void {
    const editorElement = this.editorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${this.mode}`,
      showGutter: true,
      showPrintMargin: this.showPrintMargin,
      readOnly: this.disabled || this.readonly,
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce(this.mode, 'textmate').subscribe(
      (ace) => {
        this.editor = ace.edit(editorElement, editorOptions);
        this.editor.session.setUseWrapMode(true);
        this.editor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.editor.setReadOnly(this.disabled);
        this.editor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        // @ts-ignore
        this.editor.session.on('changeAnnotation', () => {
          const annotations = this.editor.session.getAnnotations();
          const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
          if (this.hasErrors !== hasErrors) {
            this.hasErrors = hasErrors;
            this.propagateChange(this.modelValue);
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
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.editor.resize();
      this.editor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.editor) {
      this.editor.setReadOnly(this.disabled);
    }
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
    this.modelValue = value;
    if (this.editor) {
      this.ignoreChange = true;
      this.editor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.editor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.propagateChange(this.modelValue);
    }
  }
}
