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

import { Component, ElementRef, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
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
import { Observable, of } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  toCustomAction,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetActionSource, WidgetActionType, widgetActionTypeTranslationMap } from '@shared/models/widget.models';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { DashboardService } from '@core/http/dashboard.service';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

export interface WidgetActionDialogData {
  isAdd: boolean;
  callbacks: WidgetActionCallbacks;
  actionsData: WidgetActionsData;
  action?: WidgetActionDescriptorInfo;
}

@Component({
  selector: 'tb-widget-action-dialog',
  templateUrl: './widget-action-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: WidgetActionDialogComponent}],
  styleUrls: ['./widget-action-dialog.component.scss']
})
export class WidgetActionDialogComponent extends DialogComponent<WidgetActionDialogComponent,
                                                 WidgetActionDescriptorInfo> implements OnInit, ErrorStateMatcher {

  @ViewChild('dashboardStateInput') dashboardStateInput: ElementRef;

  widgetActionFormGroup: FormGroup;
  actionTypeFormGroup: FormGroup;

  isAdd: boolean;
  action: WidgetActionDescriptorInfo;

  widgetActionTypes = Object.keys(WidgetActionType);
  widgetActionTypeTranslations = widgetActionTypeTranslationMap;
  widgetActionType = WidgetActionType;

  filteredDashboardStates: Observable<Array<string>>;
  targetDashboardStateSearchText = '';
  selectedDashboardStateIds: Observable<Array<string>>;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private utils: UtilsService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              @Inject(MAT_DIALOG_DATA) public data: WidgetActionDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<WidgetActionDialogComponent, WidgetActionDescriptorInfo>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    if (this.isAdd) {
      this.action = {
        id: this.utils.guid(),
        name: '',
        icon: 'more_horiz',
        type: null
      };
    } else {
      this.action = this.data.action;
    }
  }

  ngOnInit(): void {
    this.widgetActionFormGroup = this.fb.group({});
    this.widgetActionFormGroup.addControl('actionSourceId',
      this.fb.control(this.action.actionSourceId, [Validators.required]));
    this.widgetActionFormGroup.addControl('name',
      this.fb.control(this.action.name, [this.validateActionName(), Validators.required]));
    this.widgetActionFormGroup.addControl('icon',
      this.fb.control(this.action.icon, [Validators.required]));
    this.widgetActionFormGroup.addControl('type',
      this.fb.control(this.action.type, [Validators.required]));
    this.updateActionTypeFormGroup(this.action.type, this.action);
    this.widgetActionFormGroup.get('type').valueChanges.subscribe((type: WidgetActionType) => {
      this.updateActionTypeFormGroup(type);
    });
    this.widgetActionFormGroup.get('actionSourceId').valueChanges.subscribe(() => {
      this.widgetActionFormGroup.get('name').updateValueAndValidity();
    });
  }

  private updateActionTypeFormGroup(type?: WidgetActionType, action?: WidgetActionDescriptorInfo) {
    this.actionTypeFormGroup = this.fb.group({});
    if (type) {
      switch (type) {
        case WidgetActionType.openDashboard:
        case WidgetActionType.openDashboardState:
        case WidgetActionType.updateDashboardState:
          this.actionTypeFormGroup.addControl(
            'targetDashboardStateId',
            this.fb.control(action ? action.targetDashboardStateId : null,
              type === WidgetActionType.openDashboardState ? [Validators.required] : [])
          );
          this.actionTypeFormGroup.addControl(
            'setEntityId',
            this.fb.control(action ? action.setEntityId : true, [])
          );
          this.actionTypeFormGroup.addControl(
            'stateEntityParamName',
            this.fb.control(action ? action.stateEntityParamName : null, [])
          );
          if (type === WidgetActionType.openDashboard) {
            this.actionTypeFormGroup.addControl(
              'targetDashboardId',
              this.fb.control(action ? action.targetDashboardId : null,
                [Validators.required])
            );
            this.setupSelectedDashboardStateIds(action ? action.targetDashboardId : null);
          } else {
            this.actionTypeFormGroup.addControl(
              'openRightLayout',
              this.fb.control(action ? action.openRightLayout : false, [])
            );
          }
          this.setupFilteredDashboardStates();
          break;
        case WidgetActionType.custom:
          this.actionTypeFormGroup.addControl(
            'customFunction',
            this.fb.control(action ? action.customFunction : null, [])
          );
          break;
        case WidgetActionType.customPretty:
          this.actionTypeFormGroup.addControl(
            'customAction',
            this.fb.control(toCustomAction(action), [Validators.required])
          );
          break;
      }
    }
  }

  private setupSelectedDashboardStateIds(targetDashboardId?: string) {
    this.selectedDashboardStateIds =
      this.actionTypeFormGroup.get('targetDashboardId').valueChanges.pipe(
        // startWith<string>(targetDashboardId),
        tap(() => {
          this.targetDashboardStateSearchText = '';
        }),
        mergeMap((dashboardId) => {
          if (dashboardId) {
            return this.dashboardService.getDashboard(dashboardId);
          } else {
            return of(null);
          }
        }),
        map((dashboard: Dashboard) => {
          if (dashboard) {
            dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
            const states = dashboard.configuration.states;
            return Object.keys(states);
          } else {
            return [];
          }
        })
      );
  }

  private setupFilteredDashboardStates() {
    this.targetDashboardStateSearchText = '';
    this.filteredDashboardStates = this.actionTypeFormGroup.get('targetDashboardStateId').valueChanges
      .pipe(
        startWith(''),
        map(value => value ? value : ''),
        mergeMap(name => this.fetchDashboardStates(name) )
      );
  }

  private fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    this.targetDashboardStateSearchText = searchText;
    if (this.widgetActionFormGroup.get('type').value === WidgetActionType.openDashboard) {
      return this.selectedDashboardStateIds.pipe(
        map(stateIds => {
          const result = searchText ? stateIds.filter(this.createFilterForDashboardState(searchText)) : stateIds;
          if (result && result.length) {
            return result;
          } else {
            return [searchText];
          }
        })
      );
    } else {
      return of(this.data.callbacks.fetchDashboardStates(searchText));
    }
  }

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }

  public clearTargetDashboardState(value: string = '') {
    this.dashboardStateInput.nativeElement.value = value;
    this.actionTypeFormGroup.get('targetDashboardStateId').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.dashboardStateInput.nativeElement.blur();
      this.dashboardStateInput.nativeElement.focus();
    }, 0);
  }

  private validateActionName(): ValidatorFn {
    return (c: FormControl) => {
      const newName = c.value;
      const valid = this.checkActionName(newName, this.widgetActionFormGroup.get('actionSourceId').value);
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

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const type: WidgetActionType = this.widgetActionFormGroup.get('type').value;
    let result: WidgetActionDescriptorInfo;
    if (type === WidgetActionType.customPretty) {
      result = {...this.widgetActionFormGroup.value, ...this.actionTypeFormGroup.get('customAction').value};
    } else {
      result = {...this.widgetActionFormGroup.value, ...this.actionTypeFormGroup.value};
    }
    result.id = this.action.id;
    this.dialogRef.close(result);
  }
}
