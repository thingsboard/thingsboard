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
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, UntypedFormControl, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { AceHighlightRules, getAce, Range } from '@shared/models/ace/ace.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, guid, isEqual, isObject, isUndefined, isUndefinedOrNull } from '@app/core/utils';
import { TranslateService } from '@ngx-translate/core';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { beautifyJs } from '@shared/models/beautify.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { compileTbFunction, loadModulesCompleter, TbFunction } from '@shared/models/js-function.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { JsFuncModulesComponent } from '@shared/components/js-func-modules.component';
import { HttpClient } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { tbelUtilsAutocompletes, tbelUtilsFuncHighlightRules } from '@shared/models/ace/tbel-utils.models';

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
export class JsFuncComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor, Validator {

  @ViewChild('javascriptEditor', {static: true})
  javascriptEditorElmRef: ElementRef;

  private jsEditor: Ace.Editor;
  private initialCompleters: Ace.Completer[];
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `jsFuncEditor-${guid()}`;

  @Input() label: string;

  @Input() functionTitle: string;

  @Input() functionName: string;

  @Input() functionArgs: Array<string>;

  @Input() validationArgs: Array<any>;

  @Input() resultType: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() minHeight = '200px';

  @Input() editorCompleter: TbEditorCompleter;

  @Input() highlightRules: AceHighlightRules;

  @Input() globalVariables: Array<string>;

  @Input() helpPopupStyle: Record<string, any> = {};

  @Input()
  @coerceBoolean()
  disableUndefinedCheck = false;

  @Input() helpId: string;

  @Input() scriptLanguage: ScriptLanguage = ScriptLanguage.JS;

  @Input()
  @coerceBoolean()
  hideBrackets = false;

  @Input()
  @coerceBoolean()
  hideLabel = false;

  @Input()
  @coerceBoolean()
  withModules = false;

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

  functionLabel: string;

  fullscreen = false;

  modelValue: string;

  modules: {[alias: string]: string };

  functionValid = true;

  validationError: string;

  errorShowed = false;

  errorMarkers: number[] = [];
  errorAnnotationId = -1;

  private functionArgsString = '';

  private propagateChange = null;
  private _onTouched = null;
  public hasErrors = false;

  constructor(public elementRef: ElementRef,
              private utils: UtilsService,
              private translate: TranslateService,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private http: HttpClient) {
  }

  ngOnInit(): void {
    if (!this.resultType || this.resultType.length === 0) {
      this.resultType = 'nocheck';
    }
    this.updateFunctionArgsString()
    this.updateFunctionLabel();
    const editorElement = this.javascriptEditorElmRef.nativeElement;
    let editorOptions: Partial<Ace.EditorOptions> = {
        mode: 'ace/mode/javascript',
        showGutter: true,
        showPrintMargin: true,
        readOnly: this.disabled
    };
    if (ScriptLanguage.TBEL === this.scriptLanguage) {
      editorOptions.mode = 'ace/mode/tbel';
    }

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    getAce().subscribe(
      (ace) => {
        this.jsEditor = ace.edit(editorElement, editorOptions);
        this.jsEditor.session.setUseWrapMode(true);
        this.jsEditor.setValue(this.modelValue ? this.modelValue : '', -1);
        this.jsEditor.setReadOnly(this.disabled);
        this.jsEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupJsErrors();
            this.updateView();
          }
        });
        this.jsEditor.on('blur', () => {
          if (this._onTouched) {
            this._onTouched();
          }
        });
        if (!this.disableUndefinedCheck) {
          // @ts-ignore
          this.jsEditor.session.on('changeAnnotation', () => {
            const annotations = this.jsEditor.session.getAnnotations();
            annotations.filter(annotation => annotation.text.includes('is not defined')).forEach(annotation => {
              annotation.type = 'error';
            });
            this.jsEditor.renderer.setAnnotations(annotations);
            const hasErrors = annotations.filter(annotation => annotation.type === 'error').length > 0;
            if (this.hasErrors !== hasErrors) {
              this.hasErrors = hasErrors;
              this.propagateValue(this.modelValue);
              this.cd.markForCheck();
            }
          });
        }
        this.updateHighlightRules();
        this.updateJsWorkerGlobals();
        this.initialCompleters = this.jsEditor.completers || [];
        this.updateCompleters();
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const { firstChange, currentValue, previousValue } = changes[propName];
      const isChanged = isObject(currentValue) ? !isEqual(currentValue, previousValue) : currentValue !== previousValue;
      if (!firstChange && isChanged) {
        this.updateByChangesPropName(propName);
      }
    }
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.jsEditor) {
      this.jsEditor.destroy();
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.jsEditor.resize();
      this.jsEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.jsEditor) {
      this.jsEditor.setReadOnly(this.disabled);
    }
  }

  public validate(c: UntypedFormControl) {
    return (this.functionValid && !this.hasErrors) ? null : {
      jsFunc: {
        valid: false,
      },
    };
  }

  beautifyJs() {
    beautifyJs(this.modelValue, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.jsEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  private updateFunctionArgsString(): void {
    this.functionArgsString = '';
    if (this.functionArgs) {
      this.functionArgsString = this.functionArgs.join(', ');
    }
  }

  private updateFunctionLabel(): void {
    if (this.functionTitle || this.label) {
      this.hideBrackets = true;
    }
    if (this.functionTitle) {
      this.functionLabel = `${this.functionTitle}: f(${this.functionArgsString})`;
    } else if (this.label) {
      this.functionLabel = this.label;
    } else {
      this.functionLabel =
        `function ${this.functionName ? this.functionName : ''}(${this.functionArgsString})${this.hideBrackets ? '' : ' {'}`;
    }
    this.cd.markForCheck();
  }

  private updatedScriptLanguage() {
    this.jsEditor?.session?.setMode(`ace/mode/${ScriptLanguage.TBEL === this.scriptLanguage ? 'tbel' : 'javascript'}`);
  }

  validateOnSubmit(): Observable<void> {
    if (!this.disabled) {
      this.cleanupJsErrors();
      return this.validateJsFunc().pipe(
        map((valid) => {
          this.functionValid = valid;
          if (!this.functionValid) {
            this.propagateValue(this.modelValue);
            this.cd.markForCheck();
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
        })
      );
    } else {
      return of(null);
    }
  }

  public focus() {
    this.javascriptEditorElmRef.nativeElement.scrollIntoView();
    this.jsEditor?.focus();
  }

  private validateJsFunc(): Observable<boolean> {
    let toCompile: TbFunction;
    if (this.withModules && this.modules && Object.keys(this.modules).length) {
      toCompile = {
        body: this.modelValue,
        modules: this.modules
      };
    } else {
      toCompile = this.modelValue;
    }
    const args = this.functionArgs || [];
    return compileTbFunction(this.http, toCompile, ...args).pipe(
      map(toValidate => {
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
      }),
      catchError((e) => {
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
          const errorMarkerId = this.jsEditor.session.addMarker(new Range(line, 0, line, Infinity),
            'ace_active-line', 'screenLine');
          this.errorMarkers.push(errorMarkerId);
          const annotations = this.jsEditor.session.getAnnotations();
          const errorAnnotation: Ace.Annotation = {
            row: line,
            column,
            text: details.message,
            type: 'error'
          };
          this.errorAnnotationId = annotations.push(errorAnnotation) - 1;
          this.jsEditor.session.setAnnotations(annotations);
        }
        return of(false);
      })
    );
  }

  private cleanupJsErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
    this.errorMarkers.forEach((errorMarker) => {
      this.jsEditor.session.removeMarker(errorMarker);
    });
    this.errorMarkers.length = 0;
    if (this.errorAnnotationId > -1) {
      const annotations = this.jsEditor.session.getAnnotations();
      annotations.splice(this.errorAnnotationId, 1);
      this.jsEditor.session.setAnnotations(annotations);
      this.errorAnnotationId = -1;
    }
  }

  writeValue(value: TbFunction): void {
    if (isUndefinedOrNull(value) || typeof value === 'string') {
      this.modelValue = value as any;
      this.modules = null;
    } else {
      this.modelValue = value.body;
      this.modules = value.modules;
    }
    if (this.jsEditor) {
      if (this.withModules) {
        this.updateJsWorkerGlobals();
        this.updateCompleters();
      }
      this.ignoreChange = true;
      this.jsEditor.setValue(this.modelValue ? this.modelValue : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView(force = false) {
    const editorValue = this.jsEditor.getValue();
    if (this.modelValue !== editorValue || force) {
      this.modelValue = editorValue;
      this.functionValid = true;
      this.propagateValue(this.modelValue);
      this.cd.markForCheck();
    }
  }

  editModules($event: Event, element: Element) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const modulesPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: JsFuncModulesComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'leftTop',
        context: {
          modules: deepClone(this.modules)
        },
        isModal: true
      });
      modulesPanelPopover.tbComponentRef.instance.popover = modulesPanelPopover;
      modulesPanelPopover.tbComponentRef.instance.modulesApplied.subscribe((modules) => {
        modulesPanelPopover.hide();
        this.modules = modules;
        this.updateJsWorkerGlobals();
        this.updateCompleters();
        this.updateView(true);
      });
    }
  }

  private propagateValue(value: string) {
    if (this.withModules && this.modules && Object.keys(this.modules).length) {
      const tbFunction: TbFunction = {
        body: value,
        modules: this.modules
      };
      this.propagateChange(tbFunction);
    } else {
      this.propagateChange(value);
    }
  }

  private updateByChangesPropName(propName: string): void {
    switch (propName) {
      case 'functionArgs':
        this.updateFunctionArgsString()
        this.updateFunctionLabel();
        this.updateJsWorkerGlobals();
        break;
      case 'label':
      case 'functionTitle':
      case 'functionName':
        this.updateFunctionLabel();
        break;
      case 'scriptLanguage':
        this.updatedScriptLanguage();
        this.updateHighlightRules();
        this.updateCompleters();
        this.updateJsWorkerGlobals();
        break;
      case 'disableUndefinedCheck':
      case 'globalVariables':
        this.updateJsWorkerGlobals();
        break;
      case 'editorCompleter':
        this.updateCompleters();
        break;
      case 'highlightRules':
        this.updateHighlightRules();
        break;
    }
  }

  private updateHighlightRules(): void {
    // @ts-ignore
    if (!!this.jsEditor?.session?.$mode) {
      // @ts-ignore
      const newMode = new this.jsEditor.session.$mode.constructor();
      newMode.$highlightRules = new newMode.HighlightRules();
      if (!!this.highlightRules) {
        for(const group in this.highlightRules) {
          if(!!newMode.$highlightRules.$rules[group]) {
            newMode.$highlightRules.$rules[group].unshift(...this.highlightRules[group]);
          } else {
            newMode.$highlightRules.$rules[group] = this.highlightRules[group];
          }
        }
      }
      if (this.scriptLanguage === ScriptLanguage.TBEL) {
        newMode.$highlightRules.$rules.start = [...tbelUtilsFuncHighlightRules, ...newMode.$highlightRules.$rules.start];
      }
      const identifierRule = newMode.$highlightRules.$rules.no_regex.find(rule => Array.isArray(rule.token) && rule.token.includes('identifier'));
      if (identifierRule && identifierRule.next === 'no_regex') {
        identifierRule.next = 'start';
      }
      // @ts-ignore
      this.jsEditor.session.$onChangeMode(newMode);
    }
  }

  private updateJsWorkerGlobals() {
    // @ts-ignore
    if (!!this.jsEditor?.session?.$worker) {
      const jsWorkerOptions = {
        undef: !this.disableUndefinedCheck,
        unused: true,
        globals: {}
      };
      if (!this.disableUndefinedCheck) {
        if (this.functionArgs) {
          this.functionArgs.forEach(arg => {
            jsWorkerOptions.globals[arg] = false;
          });
        }
        if (this.withModules && this.modules) {
          Object.keys(this.modules).forEach(arg => {
            jsWorkerOptions.globals[arg] = false;
          });
        }
        if (this.globalVariables) {
          this.globalVariables.forEach(arg => {
            jsWorkerOptions.globals[arg] = false;
          });
        }
      }
      // @ts-ignore
      this.jsEditor.session.$worker.send('changeOptions', [jsWorkerOptions]);
    }
  }

  updateCompleters() {
    if (!this.jsEditor) {
      return;
    }
    let modulesCompleterObservable: Observable<TbEditorCompleter>;
    if (this.withModules) {
      modulesCompleterObservable = loadModulesCompleter(this.http, this.modules);
    } else {
      modulesCompleterObservable = of(null);
    }
    modulesCompleterObservable.subscribe((modulesCompleter) => {
      const completers: Ace.Completer[] = [];
      if (this.editorCompleter) {
        completers.push(this.editorCompleter);
      }
      if (modulesCompleter) {
        completers.push(modulesCompleter);
      }
      if (this.scriptLanguage === ScriptLanguage.TBEL) {
        completers.push(tbelUtilsAutocompletes);
      }
      completers.push(...this.initialCompleters);
      this.jsEditor.completers = completers;
    });
  }
}
