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

import { Component, DestroyRef, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  toWidgetActionDescriptor,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import {
  actionDescriptorToAction,
  CellClickColumnInfo,
  defaultWidgetAction,
  WidgetActionSource,
  WidgetActionType,
  WidgetHeaderActionButtonType,
  WidgetHeaderActionButtonTypes,
  widgetHeaderActionButtonTypeTranslationMap,
  widgetType
} from '@shared/models/widget.models';
import { CustomActionEditorCompleter } from '@home/components/widget/lib/settings/common/action/custom-action.models';
import { WidgetService } from '@core/http/widget.service';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { MatSelect } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface WidgetActionDialogData {
  isAdd: boolean;
  callbacks: WidgetActionCallbacks;
  actionsData: WidgetActionsData;
  action?: WidgetActionDescriptorInfo;
  widgetType: widgetType;
  defaultIconColor?: string;
  additionalWidgetActionTypes?: WidgetActionType[];
}

@Component({
  selector: 'tb-widget-action-dialog',
  templateUrl: './widget-action-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: WidgetActionDialogComponent}],
  styleUrls: []
})
export class WidgetActionDialogComponent extends DialogComponent<WidgetActionDialogComponent,
                                                 WidgetActionDescriptorInfo> implements OnInit, ErrorStateMatcher {

  widgetActionFormGroup: FormGroup;

  isAdd: boolean;
  action: WidgetActionDescriptorInfo;

  defaultIconColor: string;

  customActionEditorCompleter = CustomActionEditorCompleter;

  submitted = false;

  functionScopeVariables: string[];

  configuredColumns: Array<CellClickColumnInfo> = [];
  usedCellClickColumns: Array<number> = [];

  widgetHeaderActionButtonType = WidgetHeaderActionButtonType
  widgetHeaderActionButtonTypes = WidgetHeaderActionButtonTypes;
  widgetHeaderActionButtonTypeTranslationMap = widgetHeaderActionButtonTypeTranslationMap;

  @ViewChild('columnIndexSelect') columnIndexSelect: MatSelect;
  columnIndexPlaceholderText = this.translate.instant('widget-config.select-column-index');

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private utils: UtilsService,
              private widgetService: WidgetService,
              @Inject(MAT_DIALOG_DATA) public data: WidgetActionDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<WidgetActionDialogComponent, WidgetActionDescriptorInfo>,
              public fb: FormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.defaultIconColor = data.defaultIconColor;
    if (this.isAdd) {
      this.action = {
        id: this.utils.guid(),
        name: '',
        icon: 'more_horiz',
        ...defaultWidgetAction(data.widgetType !== widgetType.static)
      };
    } else {
      this.action = this.data.action;
    }
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
    if (this.action.actionSourceId === 'cellClick') {
      this.getCellClickColumnsInfo();
    }
  }

  ngOnInit(): void {
    this.widgetActionFormGroup = this.fb.group({
      actionSourceId: [this.action.actionSourceId, Validators.required],
      columnIndex: [{value: this.checkColumnIndex(this.action.columnIndex), disabled: true}, Validators.required],
      name: [this.action.name, [this.validateActionName(), Validators.required]],
      buttonType: [{ value: this.action.buttonType ?? WidgetHeaderActionButtonType.icon, disabled: true}, []],
      showIcon: [{ value: this.action.showIcon ?? true, disabled: true}, []],
      icon: [this.action.icon, Validators.required],
      buttonColor: [{ value: this.action.buttonColor ?? this.defaultIconColor, disabled: true}, []],
      buttonFillColor: [{ value: this.action.buttonFillColor ?? '#305680', disabled: true}, []],
      buttonBorderColor: [{ value: this.action.buttonBorderColor ?? '#0000001F', disabled: true}, []],
      customButtonStyle: [{ value: this.action.customButtonStyle ?? {}, disabled: true}, []],
      useShowWidgetActionFunction: [this.action.useShowWidgetActionFunction],
      showWidgetActionFunction: [this.action.showWidgetActionFunction || 'return true;'],
      widgetAction: [actionDescriptorToAction(toWidgetActionDescriptor(this.action)), Validators.required]
    });
    this.updateShowWidgetActionForm();
    if (this.widgetActionFormGroup.get('actionSourceId').value === 'headerButton') {
      this.widgetActionFormGroup.get('buttonType').enable({emitEvent: false});
      this.widgetActionFormGroup.get('buttonColor').enable({emitEvent: false});
      this.widgetActionFormGroup.get('customButtonStyle').enable({emitEvent: false});
      this.widgetHeaderButtonValidators(true);
    }
    this.widgetActionFormGroup.get('actionSourceId').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      this.widgetActionFormGroup.get('name').updateValueAndValidity();
      this.updateShowWidgetActionForm();
      if (value === 'cellClick') {
        this.widgetActionFormGroup.get('columnIndex').enable();
        this.getCellClickColumnsInfo();
      } else {
        this.widgetActionFormGroup.get('columnIndex').disable();
      }
      if (value === 'headerButton') {
        this.widgetActionFormGroup.get('buttonType').enable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonColor').enable({emitEvent: false});
        this.widgetActionFormGroup.get('customButtonStyle').enable({emitEvent: false});
        this.widgetHeaderButtonValidators(true);
      } else {
        this.widgetActionFormGroup.get('buttonType').disable({emitEvent: false});
        this.widgetActionFormGroup.get('showIcon').disable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonColor').disable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonFillColor').disable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonBorderColor').disable({emitEvent: false});
        this.widgetActionFormGroup.get('customButtonStyle').disable({emitEvent: false});
      }
    });
    this.widgetActionFormGroup.get('useShowWidgetActionFunction').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateShowWidgetActionForm();
    });
    this.widgetActionFormGroup.get('buttonType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.widgetHeaderButtonValidators());
    setTimeout(() => {
      if (this.action?.actionSourceId === 'cellClick') {
        this.widgetActionFormGroup.get('columnIndex').enable();
        if (isDefinedAndNotNull(this.action.columnIndex) && this.widgetActionFormGroup.get('columnIndex').value === null) {
          this.columnIndexPlaceholderText = `${this.action.columnIndex} (${this.translate.instant('widget-config.not-set')})`;
          this.columnIndexSelect.focus();
        }
      }
    });
  }

  private widgetHeaderButtonValidators(ignoreUpdatedButtonColor = false) {
    const buttonType = this.widgetActionFormGroup.get('buttonType').value;
    if (!ignoreUpdatedButtonColor) {
      if ([WidgetHeaderActionButtonType.raised, WidgetHeaderActionButtonType.flat, WidgetHeaderActionButtonType.miniFab].includes(buttonType)) {
        this.widgetActionFormGroup.get('buttonColor').patchValue('#ffffff', {emitEvent: false});
      } else if ([WidgetHeaderActionButtonType.stroked].includes(buttonType)) {
        this.widgetActionFormGroup.get('buttonColor').patchValue('#305680', {emitEvent: false});
      } else {
        this.widgetActionFormGroup.get('buttonColor').patchValue(this.defaultIconColor, {emitEvent: false});
      }
    }
    this.widgetActionFormGroup.get('showIcon').disable({emitEvent: false});
    this.widgetActionFormGroup.get('buttonFillColor').disable({emitEvent: false});
    this.widgetActionFormGroup.get('buttonBorderColor').disable({emitEvent: false});
    switch (buttonType) {
      case WidgetHeaderActionButtonType.basic:
        this.widgetActionFormGroup.get('showIcon').enable({emitEvent: false});
        break;
      case WidgetHeaderActionButtonType.raised:
        this.widgetActionFormGroup.get('showIcon').enable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonFillColor').enable({emitEvent: false});
        break;
      case WidgetHeaderActionButtonType.stroked:
        this.widgetActionFormGroup.get('showIcon').enable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonBorderColor').enable({emitEvent: false});
        break;
      case WidgetHeaderActionButtonType.flat:
        this.widgetActionFormGroup.get('showIcon').enable({emitEvent: false});
        this.widgetActionFormGroup.get('buttonFillColor').enable({emitEvent: false});
        break;
      case WidgetHeaderActionButtonType.miniFab:
        this.widgetActionFormGroup.get('buttonFillColor').enable({emitEvent: false});
        break;
    }
  }

  displayShowWidgetActionForm(): boolean {
    return !!this.data.actionsData.actionSources[this.widgetActionFormGroup.get('actionSourceId').value]?.hasShowCondition;
  }

  getWidgetActionFunctionHelpId(): string | undefined {
    const actionSourceId = this.widgetActionFormGroup.get('actionSourceId').value;
    if (actionSourceId === 'headerButton') {
      return 'widget/action/show_widget_action_header_fn';
    } else if (actionSourceId === 'actionCellButton') {
      return 'widget/action/show_widget_action_cell_fn';
    }
    return undefined;
  }

  private updateShowWidgetActionForm() {
    const actionSourceId = this.widgetActionFormGroup.get('actionSourceId').value;
    const useShowWidgetActionFunction = this.widgetActionFormGroup.get('useShowWidgetActionFunction').value;
    if (!!this.data.actionsData.actionSources[actionSourceId]?.hasShowCondition && useShowWidgetActionFunction) {
      this.widgetActionFormGroup.get('showWidgetActionFunction').setValidators([Validators.required]);
    } else {
      this.widgetActionFormGroup.get('showWidgetActionFunction').clearValidators();
    }
    this.widgetActionFormGroup.get('showWidgetActionFunction').updateValueAndValidity();
  }

  private checkColumnIndex(columnIndex: number): number | null {
    return isDefinedAndNotNull(columnIndex) && this.configuredColumns.length - 1 < columnIndex ? null : columnIndex;
  }

  private getCellClickColumnsInfo(): void {
    if (!this.configuredColumns.length) {
      this.configuredColumns = this.data.callbacks.fetchCellClickColumns();
      this.data.actionsData.actionsMap['cellClick']?.forEach(action => {
        const actionColumn = this.configuredColumns[action.columnIndex];
        if (actionColumn && action.columnIndex !== this.action.columnIndex) {
          this.usedCellClickColumns.push(action.columnIndex);
        }
      });
    }
  }

  private validateActionName(): ValidatorFn {
    return (c: FormControl) => {
      const newName = c.value;
      const valid = this.checkActionName(newName, c.parent?.get('actionSourceId').value);
      return !valid ? {
        actionNameNotUnique: true
      } : null;
    };
  }

  private checkActionName(name: string, actionSourceId: string): boolean {
    let actionNameIsUnique = true;
    if (name && actionSourceId) {
      const sourceActions = this.data.actionsData.actionsMap[actionSourceId];
      if (sourceActions) {
        const result = sourceActions.filter((sourceAction) => sourceAction.name === name);
        if (result && result.length && result[0].id !== this.action.id) {
          actionNameIsUnique = false;
        }
      }
    }
    return actionNameIsUnique;
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  public actionSourceName(actionSource: WidgetActionSource): string {
    if (actionSource) {
      return this.utils.customTranslation(actionSource.name, actionSource.name);
    } else {
      return '';
    }
  }

  public getCellClickColumnInfo(index: number, columnInfo: CellClickColumnInfo): string {
    return `${index} (${isNotEmptyStr(columnInfo.label) ? columnInfo.label : columnInfo.name})`;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.widgetActionFormGroup.valid) {
      const result: WidgetActionDescriptorInfo =
        {...this.widgetActionFormGroup.value, ...this.widgetActionFormGroup.get('widgetAction').value};
      delete (result as any).widgetAction;
      result.id = this.action.id;
      this.dialogRef.close(result);
    }
  }
}
