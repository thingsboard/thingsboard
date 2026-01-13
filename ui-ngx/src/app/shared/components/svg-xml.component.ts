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
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-svg-xml',
  templateUrl: './svg-xml.component.html',
  styleUrls: ['./svg-xml.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SvgXmlComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SvgXmlComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class SvgXmlComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('svgXmlEditor', {static: true})
  svgXmlEditorElmRef: ElementRef;

  private svgXmlEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input()
  @coerceBoolean()
  fillHeight = false;

  @Input()
  @coerceBoolean()
  noLabel = false;

  @Input() minHeight = '200px';

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
    const editorElement = this.svgXmlEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: 'ace/mode/svg',
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
        this.svgXmlEditor = ace.edit(editorElement, editorOptions);
        this.svgXmlEditor.session.setUseWrapMode(true);
        this.svgXmlEditor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.svgXmlEditor.setReadOnly(this.disabled);
        this.svgXmlEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.updateView();
          }
        });
        // @ts-ignore
        this.svgXmlEditor.session.on('changeAnnotation', () => {
          const annotations = this.svgXmlEditor.session.getAnnotations();
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
    if (this.svgXmlEditor) {
      this.svgXmlEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.svgXmlEditor.resize();
      this.svgXmlEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.svgXmlEditor) {
      this.svgXmlEditor.setReadOnly(this.disabled);
    }
  }

  public validate(c: UntypedFormControl) {
    return (!this.hasErrors) ? null : {
      svgXml: {
        valid: false,
      },
    };
  }

  writeValue(value: string): void {
    this.modelValue = value;
    if (this.svgXmlEditor) {
      this.ignoreChange = true;
      this.svgXmlEditor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.svgXmlEditor.getValue();
    if (this.modelValue !== editorValue) {
      this.modelValue = editorValue;
      this.propagateChange(this.modelValue);
      this.cd.markForCheck();
    }
  }
}
