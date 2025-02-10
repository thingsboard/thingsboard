///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  ViewChild,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
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
import { CalculatedFieldScriptTestDialogData } from '@shared/models/calculated-field.models';

@Component({
  selector: 'tb-calculated-field-script-test-dialog',
  templateUrl: './calculated-field-script-test-dialog.component.html',
  styleUrls: ['./calculated-field-script-test-dialog.component.scss'],
})
export class CalculatedFieldScriptTestDialogComponent extends DialogComponent<CalculatedFieldScriptTestDialogComponent,
  string> implements AfterViewInit {

  @ViewChild('leftPanel', {static: true}) leftPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('rightPanel', {static: true}) rightPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('topRightPanel', {static: true}) topRightPanelElmRef: ElementRef<HTMLElement>;
  @ViewChild('bottomRightPanel', {static: true}) bottomRightPanelElmRef: ElementRef<HTMLElement>;

  @ViewChild('expressionContent', {static: true}) expressionContent: JsonContentComponent;

  calculatedFieldScriptTestFormGroup = this.fb.group({
    expression: [],
    arguments: [],
    output: []
  });

  readonly ContentType = ContentType;
  readonly ScriptLanguage = ScriptLanguage;
  readonly functionArgs = Object.keys(this.data.arguments);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: CalculatedFieldScriptTestDialogData,
              protected dialogRef: MatDialogRef<CalculatedFieldScriptTestDialogComponent, string>,
              private fb: FormBuilder,
              private destroyRef: DestroyRef,
              private calculatedFieldService: CalculatedFieldsService) {
    super(store, router, dialogRef);
    beautifyJs(this.data.expression, {indent_size: 4}).pipe(takeUntilDestroyed()).subscribe(
      (res) => {
        this.calculatedFieldScriptTestFormGroup.get('expression').patchValue(res, {emitEvent: false});
      }
    );
    this.calculatedFieldScriptTestFormGroup.get('arguments').patchValue(this.data.arguments, {emitEvent: false});
  }

  ngAfterViewInit(): void {
    this.initSplitLayout(
      this.leftPanelElmRef.nativeElement,
      this.rightPanelElmRef.nativeElement,
      this.topRightPanelElmRef.nativeElement,
      this.bottomRightPanelElmRef.nativeElement
    );
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
    this.testScript().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.calculatedFieldScriptTestFormGroup.get('expression').markAsPristine();
      this.dialogRef.close(this.calculatedFieldScriptTestFormGroup.get('expression').value);
    });
  }

  private testScript(): Observable<string> {
    if (this.checkInputParamErrors()) {
      return this.calculatedFieldService.testScript({
        expression: this.calculatedFieldScriptTestFormGroup.get('expression').value,
        arguments: this.calculatedFieldScriptTestFormGroup.get('arguments').value
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

  private initSplitLayout(leftPanel, rightPanel, topRightPanel, bottomRightPanel): void {
    Split([leftPanel, rightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'col-resize'
    });

    Split([topRightPanel, bottomRightPanel], {
      sizes: [50, 50],
      gutterSize: 8,
      cursor: 'row-resize',
      direction: 'vertical'
    });
  }
}
