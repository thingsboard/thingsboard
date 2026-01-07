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
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  Inject,
  OnDestroy,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, Validators } from '@angular/forms';
import { NEVER, Observable, of, switchMap } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { ContentType } from '@shared/models/constants';
import { JsonContentComponent } from '@shared/components/json-content.component';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { beautifyJs } from '@shared/models/beautify.models';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs/operators';
import {
  ArgumentType,
  CalculatedFieldEventArguments,
  CalculatedFieldTestScriptInputParams,
  TestArgumentTypeMap
} from '@shared/models/calculated-field.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { AceHighlightRules } from '@shared/models/ace/ace.models';

export interface CalculatedFieldTestScriptDialogData extends CalculatedFieldTestScriptInputParams {
  argumentsEditorCompleter: TbEditorCompleter;
  argumentsHighlightRules: AceHighlightRules;
  openCalculatedFieldEdit?: boolean;
}

@Component({
  selector: 'tb-calculated-field-script-test-dialog',
  templateUrl: './calculated-field-script-test-dialog.component.html',
  styleUrls: ['./calculated-field-script-test-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CalculatedFieldScriptTestDialogComponent extends DialogComponent<CalculatedFieldScriptTestDialogComponent,
  string> implements AfterViewInit, OnDestroy {

  @ViewChild('leftPanel', {static: true}) leftPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('rightPanel', {static: true}) rightPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('topRightPanel', {static: true}) topRightPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('bottomRightPanel', {static: true}) bottomRightPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('testScriptContainer', {static: true}) testScriptContainer: ElementRef<HTMLElement>;

  @ViewChild('expressionContent', {static: true}) expressionContent: JsonContentComponent;

  calculatedFieldScriptTestFormGroup = this.fb.group({
    expression: ['', Validators.required],
    arguments: [],
    output: []
  });
  argumentsTypeMap = new Map<string, ArgumentType>();

  readonly ContentType = ContentType;
  readonly ScriptLanguage = ScriptLanguage;
  readonly functionArgs = ['ctx', ...Object.keys(this.data.arguments)];

  private testScriptResize: ResizeObserver;
  private splitObjects: SplitObject[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldTestScriptDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldScriptTestDialogComponent, string>,
              private dialog: MatDialog,
              private fb: FormBuilder,
              private destroyRef: DestroyRef,
              private calculatedFieldService: CalculatedFieldsService) {
    super(store, router, dialogRef);
    beautifyJs(this.data.expression, {indent_size: 4}).pipe(filter(Boolean), takeUntilDestroyed()).subscribe(
      (res) => this.calculatedFieldScriptTestFormGroup.get('expression').patchValue(res, {emitEvent: false})
    );
    this.calculatedFieldScriptTestFormGroup.get('arguments').patchValue(this.getArgumentsValue());
  }

  ngAfterViewInit(): void {
    this.observeResize();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.testScriptResize.disconnect();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  onTestScript(): void {
    this.testScript()
      .pipe(
        switchMap(output => beautifyJs(output, {indent_size: 4})),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(output => this.calculatedFieldScriptTestFormGroup.get('output').setValue(output));
  }

  save(): void {
    this.testScript(true).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.calculatedFieldScriptTestFormGroup.get('expression').markAsPristine();
      this.dialogRef.close(this.calculatedFieldScriptTestFormGroup.get('expression').value);
    });
  }

  private testScript(onSave = false): Observable<string> {
    if (this.checkInputParamErrors()) {
      return this.calculatedFieldService.testScript({
        expression: this.calculatedFieldScriptTestFormGroup.get('expression').value,
        arguments: this.getTestArguments()
      }).pipe(
        switchMap(result => {
          if (result.error) {
            this.store.dispatch(new ActionNotificationShow(
              {
                message: result.error,
                type: 'error'
              }));
            return NEVER;
          } else {
            if (onSave && this.data.openCalculatedFieldEdit) {
              this.dialog.closeAll();
            }
            return of(result.output);
          }
        }),
      );
    } else {
      return NEVER;
    }
  }

  private checkInputParamErrors(): boolean {
    this.expressionContent.validateOnSubmit();
    return !this.calculatedFieldScriptTestFormGroup.get('expression').invalid;
  }

  private observeResize(): void {
    this.testScriptResize = new ResizeObserver(() => {
      this.updateSizes();
    });

    this.testScriptResize.observe(this.testScriptContainer.nativeElement);
  }

  private updateSizes(): void {
    this.initSplitLayout(this.testScriptContainer.nativeElement.clientWidth <= 960);
  }

  private getTestArguments(): CalculatedFieldEventArguments {
    const argumentsValue = this.calculatedFieldScriptTestFormGroup.get('arguments').value;
    return Object.keys(argumentsValue)
      .reduce((acc, key) => {
        acc[key] = argumentsValue[key];
        acc[key].type = TestArgumentTypeMap.get(this.argumentsTypeMap.get(key));
        return acc;
      }, {});
  }

  private getArgumentsValue(): CalculatedFieldEventArguments {
    return Object.keys(this.data.arguments)
      .reduce((acc, key) => {
        const { type, ...argumentObj } = this.data.arguments[key];
        this.argumentsTypeMap.set(key, type);
        acc[key] = argumentObj;
        return acc;
      }, {});
  }

  private initSplitLayout(smallMode = false): void {
    const [leftPanel, rightPanel, topRightPanel, bottomRightPanel] = [
      this.leftPanelElmRef.nativeElement,
      this.rightPanelElmRef.nativeElement,
      this.topRightPanelElmRef.nativeElement,
      this.bottomRightPanelElmRef.nativeElement
    ] as unknown as string[];

    this.splitObjects.forEach(obj => obj.destroy());
    this.splitObjects = [];

    if (smallMode) {
      this.splitObjects.push(
        Split([leftPanel, rightPanel], {
          sizes: [33, 67],
          gutterSize: 8,
          cursor: 'row-resize',
          direction: 'vertical'
        }),
        Split([topRightPanel, bottomRightPanel], {
          sizes: [50, 50],
          gutterSize: 8,
          cursor: 'row-resize',
          direction: 'vertical'
        }),
      );
    } else {
      this.splitObjects.push(
        Split([leftPanel, rightPanel], {
          sizes: [50, 50],
          gutterSize: 8,
          cursor: 'col-resize'
        }),
        Split([topRightPanel, bottomRightPanel], {
          sizes: [50, 50],
          gutterSize: 8,
          cursor: 'row-resize',
          direction: 'vertical'
        })
      );
    }
  }
}
