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
  ControlValueAccessor,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Component, computed, ElementRef, forwardRef, input, Input, OnInit, ViewChild } from '@angular/core';
import {
  MapItemType,
  mapItemTypeTranslationMap,
  WidgetAction,
  WidgetActionType,
  widgetActionTypes,
  widgetActionTypeTranslationMap,
  widgetType
} from '@shared/models/widget.models';
import { WidgetService } from '@core/http/widget.service';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { map, mergeMap, share, startWith, takeUntil, tap } from 'rxjs/operators';
import { Observable, of, ReplaySubject, Subject, Subscription } from 'rxjs';
import { Dashboard } from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { isDefinedAndNotNull } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { PopoverPlacement, PopoverPlacements } from '@shared/components/popover.models';
import {
  CustomActionEditorCompleter,
  toCustomAction,
  toPlaceMapItemAction
} from '@home/components/widget/lib/settings/common/action/custom-action.models';
import { coerceBoolean } from '@shared/decorators/coercion';

const stateDisplayTypes = ['normal', 'separateDialog', 'popover'] as const;
type stateDisplayTypeTuple = typeof  stateDisplayTypes;
export type stateDisplayType = stateDisplayTypeTuple[number];

const stateDisplayTypesTranslations = new Map<stateDisplayType, string>(
  [
    ['normal', 'widget-action.open-normal'],
    ['separateDialog', 'widget-action.open-in-separate-dialog'],
    ['popover', 'widget-action.open-in-popover'],
  ]
);

@Component({
  selector: 'tb-widget-action',
  templateUrl: './widget-action.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetActionComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => WidgetActionComponent),
      multi: true,
    }
  ]
})
export class WidgetActionComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('dashboardStateInput', {static: false}) dashboardStateInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  widgetType: widgetType;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  @coerceBoolean()
  withName = false;

  @Input()
  actionNames: string[];

  additionalWidgetActionTypes = input<WidgetActionType[]>(null);

  actionTypes = computed(() => {
    const predefinedActionTypes = widgetActionTypes;
    if (this.additionalWidgetActionTypes()?.length) {
      return predefinedActionTypes.concat(this.additionalWidgetActionTypes());
    }
    return predefinedActionTypes;
  });

  widgetActionTypeTranslations = widgetActionTypeTranslationMap;
  widgetActionType = WidgetActionType;

  mapItemTypes = Object.values(MapItemType) as MapItemType[];
  mapItemTypeTranslationMap = mapItemTypeTranslationMap;

  allStateDisplayTypes = stateDisplayTypes;
  allPopoverPlacements = PopoverPlacements;

  WidgetType = widgetType;

  filteredDashboardStates: Observable<Array<string>>;
  targetDashboardStateSearchText = '';
  selectedDashboardStateIds: Observable<Array<string>>;

  customActionEditorCompleter = CustomActionEditorCompleter;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  widgetActionFormGroup: UntypedFormGroup;
  actionTypeFormGroup: UntypedFormGroup;
  stateDisplayTypeFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};
  private actionTypeFormGroupSubscriptions: Subscription[] = [];
  private stateDisplayTypeFormGroupSubscriptions: Subscription[] = [];
  private destroy$ = new Subject<void>();
  private dashboard: Dashboard;

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.widgetActionFormGroup.disable({emitEvent: false});
      if (this.actionTypeFormGroup) {
        this.actionTypeFormGroup.disable({emitEvent: false});
      }
      if (this.stateDisplayTypeFormGroup) {
        this.stateDisplayTypeFormGroup.disable({emitEvent: false});
      }
    } else {
      this.widgetActionFormGroup.enable({emitEvent: false});
    }
  }

  ngOnInit() {
    this.widgetActionFormGroup = this.fb.group({});
    if (this.withName) {
      this.widgetActionFormGroup.addControl('name',
        this.fb.control(null, [this.validateActionName(), Validators.required]));
    }
    this.widgetActionFormGroup.addControl('type',
      this.fb.control(null, [Validators.required]));
    this.widgetActionFormGroup.get('type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: WidgetActionType) => {
      this.updateActionTypeFormGroup(type);
    });
    this.widgetActionFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.widgetActionUpdated();
    });
  }

  writeValue(widgetAction?: WidgetAction): void {
    if (this.withName) {
      this.widgetActionFormGroup.patchValue({
        name: widgetAction?.name
      }, {emitEvent: false});
    }
    this.widgetActionFormGroup.patchValue({
      type: widgetAction?.type
    }, {emitEvent: false});
    this.updateActionTypeFormGroup(widgetAction?.type, widgetAction);
  }

  validate(_c: UntypedFormControl) {
    return (this.widgetActionFormGroup.valid &&
      this.actionTypeFormGroup.valid && (!this.stateDisplayTypeFormGroup || this.stateDisplayTypeFormGroup.valid)) ? null : {
      widgetAction: {
        valid: false,
      }
    };
  }

  clearTargetDashboardState(value: string = '') {
    this.dashboardStateInput.nativeElement.value = value;
    this.actionTypeFormGroup.get('targetDashboardStateId').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.dashboardStateInput.nativeElement.blur();
      this.dashboardStateInput.nativeElement.focus();
    }, 0);
  }

  onDashboardStateInputFocus(): void {
    this.actionTypeFormGroup.get('targetDashboardStateId').updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  stateDisplayTypeName(displayType: stateDisplayType): string {
    if (displayType) {
      return this.translate.instant(stateDisplayTypesTranslations.get(displayType)) + '';
    } else {
      return '';
    }
  }

  popoverPlacementName(placement: PopoverPlacement): string {
    if (placement) {
      return this.translate.instant(`widget-action.popover-placement-${placement}`) + '';
    } else {
      return '';
    }
  }

  private updateActionTypeFormGroup(type?: WidgetActionType, action?: WidgetAction) {
    this.actionTypeFormGroupSubscriptions.forEach(s => s.unsubscribe());
    this.actionTypeFormGroupSubscriptions.length = 0;
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
            this.fb.control(this.widgetType === widgetType.static ? false : action ? action.setEntityId : true, [])
          );
          this.actionTypeFormGroup.addControl(
            'stateEntityParamName',
            this.fb.control(action ? action.stateEntityParamName : null, [])
          );
          if (type === WidgetActionType.openDashboard) {
            const targetDashboardId = action ? action.targetDashboardId : null;
            this.actionTypeFormGroup.addControl(
              'openNewBrowserTab',
              this.fb.control(action ? action.openNewBrowserTab : false, [])
            );
            this.actionTypeFormGroup.addControl(
              'targetDashboardId',
              this.fb.control(targetDashboardId, [Validators.required])
            );
            this.setupSelectedDashboardStateIds(targetDashboardId);
          } else {
            if (type === WidgetActionType.openDashboardState) {
              const displayType = this.getStateDisplayType(action);
              this.actionTypeFormGroup.addControl(
                'stateDisplayType',
                this.fb.control(this.getStateDisplayType(action), [Validators.required])
              );
              this.updateStateDisplayTypeFormGroup(displayType, action);
              this.actionTypeFormGroupSubscriptions.push(
                this.actionTypeFormGroup.get('stateDisplayType').valueChanges.pipe(
                  takeUntil(this.destroy$)
                ).subscribe((displayTypeValue: stateDisplayType) => {
                  this.updateStateDisplayTypeFormGroup(displayTypeValue);
                })
              );
            }
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
        case WidgetActionType.mobileAction:
          this.actionTypeFormGroup.addControl(
            'mobileAction',
            this.fb.control(action ? action.mobileAction : null, [Validators.required])
          );
          break;
        case WidgetActionType.openURL:
          this.actionTypeFormGroup.addControl(
            'openNewBrowserTab',
            this.fb.control(action ? action.openNewBrowserTab : false, [])
          );
          this.actionTypeFormGroup.addControl(
            'url',
            this.fb.control(action ? action.url : null, [Validators.required])
          );
          break;
        case WidgetActionType.placeMapItem:
          this.actionTypeFormGroup.addControl(
            'mapItemType',
            this.fb.control(action?.mapItemType ?? MapItemType.marker, [Validators.required])
          );
          this.actionTypeFormGroup.addControl('mapItemTooltips', this.fb.control(action?.mapItemTooltips ?? {}));
          this.actionTypeFormGroup.addControl(
            'customAction',
            this.fb.control(toPlaceMapItemAction(action), [Validators.required])
          );
          break;
      }
    }
    this.actionTypeFormGroupSubscriptions.push(
      this.actionTypeFormGroup.valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => {
        this.widgetActionUpdated();
      })
    );
  }

  private updateStateDisplayTypeFormGroup(displayType?: stateDisplayType, action?: WidgetAction) {
    this.stateDisplayTypeFormGroupSubscriptions.forEach(s => s.unsubscribe());
    this.stateDisplayTypeFormGroupSubscriptions.length = 0;
    this.stateDisplayTypeFormGroup = this.fb.group({});
    if (displayType) {
      switch (displayType) {
        case 'normal':
          break;
        case 'separateDialog':
          this.stateDisplayTypeFormGroup.addControl(
            'dialogTitle',
            this.fb.control(action ? action.dialogTitle : '', [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'dialogHideDashboardToolbar',
            this.fb.control(action && isDefinedAndNotNull(action.dialogHideDashboardToolbar)
              ? action.dialogHideDashboardToolbar : true, [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'dialogWidth',
            this.fb.control(action ? action.dialogWidth : null, [Validators.min(1), Validators.max(100)])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'dialogHeight',
            this.fb.control(action ? action.dialogHeight : null, [Validators.min(1), Validators.max(100)])
          );
          break;
        case 'popover':
          this.stateDisplayTypeFormGroup.addControl(
            'popoverPreferredPlacement',
            this.fb.control(action && isDefinedAndNotNull(action.popoverPreferredPlacement)
              ? action.popoverPreferredPlacement : 'top', [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'popoverHideOnClickOutside',
            this.fb.control(action && isDefinedAndNotNull(action.popoverHideOnClickOutside)
              ? action.popoverHideOnClickOutside : true, [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'popoverHideDashboardToolbar',
            this.fb.control(action && isDefinedAndNotNull(action.popoverHideDashboardToolbar)
              ? action.popoverHideDashboardToolbar : true, [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'popoverWidth',
            this.fb.control(action && isDefinedAndNotNull(action.popoverWidth) ? action.popoverWidth : '25vw', [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'popoverHeight',
            this.fb.control(action && isDefinedAndNotNull(action.popoverHeight) ? action.popoverHeight : '25vh', [])
          );
          this.stateDisplayTypeFormGroup.addControl(
            'popoverStyle',
            this.fb.control(action && isDefinedAndNotNull(action.popoverStyle) ? action.popoverStyle : {}, [])
          );
          break;
      }
    }
    this.stateDisplayTypeFormGroupSubscriptions.push(
      this.stateDisplayTypeFormGroup.valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(() => {
        this.widgetActionUpdated();
      })
    );
  }

  private setupSelectedDashboardStateIds(targetDashboardId: string | null) {
    this.selectedDashboardStateIds =
      this.actionTypeFormGroup.get('targetDashboardId').valueChanges.pipe(
        startWith(targetDashboardId),
        tap((dashboardId) => {
          if (!dashboardId) {
            this.actionTypeFormGroup.get('targetDashboardStateId')
            .patchValue('', {emitEvent: true});
          }

          this.targetDashboardStateSearchText = '';
        }),
        mergeMap((dashboardId) => {
          if (dashboardId) {
            if (this.dashboard?.id.id === dashboardId) {
              return of(this.dashboard);
            } else {
              return this.dashboardService.getDashboard(dashboardId);
            }
          } else {
            return of(null);
          }
        }),
        map((dashboard: Dashboard) => {
          if (dashboard) {
            if (this.dashboard?.id.id !== dashboard.id.id) {
              this.dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
            }

            return Object.keys(this.dashboard.configuration.states);
          } else {
            return [];
          }
        }),
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
  }

  private setupFilteredDashboardStates() {
    this.targetDashboardStateSearchText = '';
    this.filteredDashboardStates = this.actionTypeFormGroup.get('targetDashboardStateId').valueChanges
    .pipe(
      startWith(''),
      map(value => value ? value : ''),
      mergeMap(name => this.fetchDashboardStates(name)),
      takeUntil(this.destroy$)
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
      return of(this.callbacks.fetchDashboardStates(searchText));
    }
  }

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }

  private getStateDisplayType(action?: WidgetAction): stateDisplayType {
    let res: stateDisplayType = 'normal';
    if (action) {
      if (action.openInSeparateDialog) {
        res = 'separateDialog';
      } else if (action.openInPopover) {
        res = 'popover';
      }
    }
    return res;
  }

  private validateActionName(): ValidatorFn {
    return (c: FormControl) => {
      const newName = c.value;
      const valid = this.checkActionName(newName);
      return !valid ? {
        actionNameNotUnique: true
      } : null;
    };
  }

  private checkActionName(name: string): boolean {
    let actionNameIsUnique = true;
    if (this.actionNames?.length) {
      actionNameIsUnique = !this.actionNames.includes(name);
    }
    return actionNameIsUnique;
  }

  private widgetActionUpdated() {
    const type: WidgetActionType = this.widgetActionFormGroup.get('type').value;
    let result: WidgetAction;
    if (type === WidgetActionType.customPretty) {
      result = {...this.widgetActionFormGroup.value, ...this.actionTypeFormGroup.get('customAction').value};
    } else if (type === WidgetActionType.placeMapItem) {
      result = {
        ...this.widgetActionFormGroup.value,
        ...this.actionTypeFormGroup.get('customAction').value,
        mapItemType: this.actionTypeFormGroup.get('mapItemType').value,
        mapItemTooltips: this.actionTypeFormGroup.get('mapItemTooltips').value,
      };
    } else {
      result = {...this.widgetActionFormGroup.value, ...this.actionTypeFormGroup.value};
    }
    if (this.actionTypeFormGroup.get('stateDisplayType') &&
      this.actionTypeFormGroup.get('stateDisplayType').value !== 'normal') {
      result = {...result, ...this.stateDisplayTypeFormGroup.value};
      result.openInSeparateDialog = this.actionTypeFormGroup.get('stateDisplayType').value === 'separateDialog';
      result.openInPopover = this.actionTypeFormGroup.get('stateDisplayType').value === 'popover';
    } else {
      result.openInSeparateDialog = false;
      result.openInPopover = false;
    }
    delete (result as any).stateDisplayType;
    this.propagateChange(result);
  }
}
