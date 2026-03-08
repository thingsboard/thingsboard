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
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { getAce } from '@shared/models/ace/ace.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { beautifyCss } from '@shared/models/beautify.models';

@Component({
    selector: 'tb-css',
    templateUrl: './css.component.html',
    styleUrls: ['./css.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CssComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CssComponent),
            multi: true,
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CssComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('cssEditor', {static: true})
  cssEditorElmRef: ElementRef;

  private cssEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  modelValue: string;

  hasErrors = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const editorElement = this.cssEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/css',
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
    getAce().subscribe(
      (ace) => {
        this.cssEditor = ace.edit(editorElement, editorOptions);
        this.cssEditor.session.setUseWrapMode(true);
        this.cssEditor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.cssEditor.setReadOnly(this.disabled);
        this.cssEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        // @ts-ignore
        this.cssEditor.session.on('changeAnnotation', () => {
          const annotations = this.cssEditor.session.getAnnotations();
          const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
          if (this.hasErrors !== hasErrors) {
            this.hasErrors = hasErrors;
            this.propagateChange(this.modelValue);
            this.cd.markForCheck();
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
    if (this.cssEditor) {
      this.cssEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.cssEditor.resize();
      this.cssEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.cssEditor) {
      this.cssEditor.setReadOnly(this.disabled);
    }
  }

  public validate(c: UntypedFormControl) {
    return (!this.hasErrors) ? null : {
      css: {
        valid: false,
      },
    };
  }

  beautifyCss() {
    beautifyCss(this.modelValue, {indent_size: 4}).subscribe(
      (res) => {
        if (this.modelValue !== res) {
          this.cssEditor.setValue(res ? res : '', -1);
          this.updateView();
        }
      }
    );
  }

  writeValue(value: string): void {
    this.modelValue = value;
    if (this.cssEditor) {
      this.ignoreChange = true;
      this.cssEditor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.cssEditor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.propagateChange(this.modelValue);
      this.cd.markForCheck();
    }
  }
}
