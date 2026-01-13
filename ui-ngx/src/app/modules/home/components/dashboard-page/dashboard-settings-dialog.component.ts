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

import { Component, Inject, OnDestroy, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  BreakpointId,
  DashboardSettings,
  GridSettings,
  LayoutType,
  StateControllerId,
  ViewFormatType,
  viewFormatTypes,
  viewFormatTypeTranslationMap
} from '@app/shared/models/dashboard.models';
import { isDefined, isUndefined } from '@core/utils';
import { StatesControllerService } from './states/states-controller.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { merge, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface DashboardSettingsDialogData {
  settings?: DashboardSettings;
  gridSettings?: GridSettings;
  isRightLayout?: boolean;
  breakpointId?: BreakpointId;
}

@Component({
  selector: 'tb-dashboard-settings-dialog',
  templateUrl: './dashboard-settings-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DashboardSettingsDialogComponent}],
  styleUrls: ['./dashboard-settings-dialog.component.scss']
})
export class DashboardSettingsDialogComponent extends DialogComponent<DashboardSettingsDialogComponent, DashboardSettingsDialogData>
  implements OnDestroy, ErrorStateMatcher {

  viewFormatTypes = viewFormatTypes;
  viewFormatTypeTranslationMap = viewFormatTypeTranslationMap;

  settings: DashboardSettings;
  gridSettings: GridSettings;
  isRightLayout = false;

  settingsFormGroup: FormGroup;
  gridSettingsFormGroup: FormGroup;

  stateControllerIds: string[];

  layoutSettingsType: string;

  submitted = false;

  scadaColumns: number[] = [];

  private layoutType = LayoutType.default;
  private breakpointId: BreakpointId = 'default';

  private destroy$ = new Subject<void>();

  private stateControllerTranslationMap = new Map<string, string>([
    ['default', 'dashboard.state-controller-default'],
  ]);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardSettingsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DashboardSettingsDialogComponent, DashboardSettingsDialogData>,
              private fb: FormBuilder,
              private translate: TranslateService,
              private statesControllerService: StatesControllerService,
              private dashboardUtils: DashboardUtilsService) {
    super(store, router, dialogRef);

    this.stateControllerIds = Object.keys(this.statesControllerService.getStateControllers());

    this.settings = this.data.settings;
    this.gridSettings = this.data.gridSettings;
    this.isRightLayout = this.data.isRightLayout;
    this.breakpointId = this.data.breakpointId;

    if (this.settings) {
      const showTitle = isUndefined(this.settings.showTitle) ? true : this.settings.showTitle;
      const showDashboardLogo = isUndefined(this.settings.showDashboardLogo) ? false : this.settings.showDashboardLogo;
      const hideToolbar = isUndefined(this.settings.hideToolbar) ? false : this.settings.hideToolbar;
      this.settingsFormGroup = this.fb.group({
        stateControllerId: [isUndefined(this.settings.stateControllerId) ? 'entity' : this.settings.stateControllerId, []],
        showTitle: [showTitle, []],
        titleColor: [{value: isUndefined(this.settings.titleColor) ? 'rgba(0,0,0,0.870588)' : this.settings.titleColor,
                      disabled: !showTitle}, []],
        showDashboardLogo: [showDashboardLogo, []],
        dashboardLogoUrl: [{value: isUndefined(this.settings.dashboardLogoUrl) ? null : this.settings.dashboardLogoUrl,
                            disabled: !showDashboardLogo}, []],
        hideToolbar: [hideToolbar, []],
        toolbarAlwaysOpen: [{value: isUndefined(this.settings.toolbarAlwaysOpen) ? true : this.settings.toolbarAlwaysOpen,
          disabled: hideToolbar}, []],
        showDashboardsSelect: [{value: isUndefined(this.settings.showDashboardsSelect) ? true : this.settings.showDashboardsSelect,
          disabled: hideToolbar}, []],
        showEntitiesSelect: [{value: isUndefined(this.settings.showEntitiesSelect) ? true : this.settings.showEntitiesSelect,
          disabled: hideToolbar}, []],
        showFilters: [{value: isUndefined(this.settings.showFilters) ? true : this.settings.showFilters,
          disabled: hideToolbar}, []],
        showDashboardTimewindow: [{value: isUndefined(this.settings.showDashboardTimewindow) ? true : this.settings.showDashboardTimewindow,
          disabled: hideToolbar}, []],
        showDashboardExport: [{value: isUndefined(this.settings.showDashboardExport) ? true : this.settings.showDashboardExport,
          disabled: hideToolbar}, []],
        showUpdateDashboardImage: [
          {value: isUndefined(this.settings.showUpdateDashboardImage) ? true : this.settings.showUpdateDashboardImage,
          disabled: hideToolbar}, []],
        dashboardCss: [isUndefined(this.settings.dashboardCss) ? '' : this.settings.dashboardCss, []],
      });
      this.settingsFormGroup.get('stateControllerId').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(
        (stateControllerId: StateControllerId) => {
          if (stateControllerId !== 'default') {
            this.settingsFormGroup.get('toolbarAlwaysOpen').setValue(true);
          }
        }
      );
      this.settingsFormGroup.get('showTitle').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(
        (showTitleValue: boolean) => {
          if (showTitleValue) {
            this.settingsFormGroup.get('titleColor').enable();
          } else {
            this.settingsFormGroup.get('titleColor').disable();
          }
        }
      );
      this.settingsFormGroup.get('showDashboardLogo').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(
        (showDashboardLogoValue: boolean) => {
          if (showDashboardLogoValue) {
            this.settingsFormGroup.get('dashboardLogoUrl').enable();
          } else {
            this.settingsFormGroup.get('dashboardLogoUrl').disable();
          }
        }
      );
      this.settingsFormGroup.get('hideToolbar').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe(
        (hideToolbarValue: boolean) => {
          if (hideToolbarValue) {
            this.settingsFormGroup.get('toolbarAlwaysOpen').disable();
            this.settingsFormGroup.get('showDashboardsSelect').disable();
            this.settingsFormGroup.get('showEntitiesSelect').disable();
            this.settingsFormGroup.get('showFilters').disable();
            this.settingsFormGroup.get('showDashboardTimewindow').disable();
            this.settingsFormGroup.get('showDashboardExport').disable();
            this.settingsFormGroup.get('showUpdateDashboardImage').disable();
          } else {
            this.settingsFormGroup.get('toolbarAlwaysOpen').enable();
            this.settingsFormGroup.get('showDashboardsSelect').enable();
            this.settingsFormGroup.get('showEntitiesSelect').enable();
            this.settingsFormGroup.get('showFilters').enable();
            this.settingsFormGroup.get('showDashboardTimewindow').enable();
            this.settingsFormGroup.get('showDashboardExport').enable();
            this.settingsFormGroup.get('showUpdateDashboardImage').enable();
          }
        }
      );
    } else {
      this.settingsFormGroup = this.fb.group({});
    }

    if (this.gridSettings) {
      if (isDefined(this.gridSettings.layoutType)) {
        this.layoutType = this.gridSettings.layoutType;
      }
      if (this.layoutType !== LayoutType.divider) {
        this.layoutSettingsType = this.dashboardUtils.getBreakpointName(this.breakpointId);
      } else if (this.isRightLayout) {
        this.layoutSettingsType = this.translate.instant('layout.right');
      } else {
        this.layoutSettingsType = this.translate.instant('layout.left');
      }
      let columns = 24;
      while (columns <= 1008) {
        this.scadaColumns.push(columns);
        columns += 24;
      }
      const mobileAutoFillHeight = isUndefined(this.gridSettings.mobileAutoFillHeight) ? false : this.gridSettings.mobileAutoFillHeight;
      this.gridSettingsFormGroup = this.fb.group({
        columns: [this.gridSettings.columns || 24, [Validators.required, Validators.min(10), Validators.max(1008)]],
        minColumns: [this.gridSettings.minColumns || this.gridSettings.columns || 24,
          [Validators.required, Validators.min(10), Validators.max(1008)]],
        margin: [isDefined(this.gridSettings.margin) ? this.gridSettings.margin : 10,
          [Validators.required, Validators.min(0), Validators.max(50)]],
        outerMargin: [isUndefined(this.gridSettings.outerMargin) ? true : this.gridSettings.outerMargin, []],
        viewFormat: [isUndefined(this.gridSettings.viewFormat) ? ViewFormatType.grid : this.gridSettings.viewFormat, []],
        autoFillHeight: [isUndefined(this.gridSettings.autoFillHeight) ? false : this.gridSettings.autoFillHeight, []],
        rowHeight: [isUndefined(this.gridSettings.rowHeight) ? 70 : this.gridSettings.rowHeight,
          [Validators.required, Validators.min(5), Validators.max(200)]],
        backgroundColor: [this.gridSettings.backgroundColor || 'rgba(0,0,0,0)', []],
        backgroundImageUrl: [this.gridSettings.backgroundImageUrl, []],
        backgroundSizeMode: [this.gridSettings.backgroundSizeMode || '100%', []],
        mobileAutoFillHeight: [mobileAutoFillHeight, []],
        mobileRowHeight: [{ value: isUndefined(this.gridSettings.mobileRowHeight) ? 70 : this.gridSettings.mobileRowHeight,
          disabled: mobileAutoFillHeight}, [Validators.required, Validators.min(5), Validators.max(200)]]
      });
      if (this.isRightLayout) {
        const mobileDisplayLayoutFirst =
          isUndefined(this.gridSettings.mobileDisplayLayoutFirst) ? false : this.gridSettings.mobileDisplayLayoutFirst;
        this.gridSettingsFormGroup.addControl('mobileDisplayLayoutFirst', this.fb.control(mobileDisplayLayoutFirst, []));
      }
      if (this.isScada()) {
        this.gridSettingsFormGroup.get('margin').disable();
        this.gridSettingsFormGroup.get('outerMargin').disable();
        this.gridSettingsFormGroup.get('viewFormat').disable({emitEvent: false});
        this.gridSettingsFormGroup.get('rowHeight').disable();
        this.gridSettingsFormGroup.get('autoFillHeight').disable({emitEvent: false});
        this.gridSettingsFormGroup.get('mobileAutoFillHeight').disable({emitEvent: false});
        this.gridSettingsFormGroup.get('mobileRowHeight').disable();
        const columnsFields: number = this.gridSettingsFormGroup.get('columns').value;
        if (columnsFields % 24 !== 0) {
          const newColumns = Math.min(1008, 24 * Math.ceil(columnsFields / 24));
          this.gridSettingsFormGroup.get('columns').patchValue(newColumns);
        }
      } else {
        merge(
          this.gridSettingsFormGroup.get('viewFormat').valueChanges,
          this.breakpointId !== 'default'
           ? this.gridSettingsFormGroup.get('autoFillHeight').valueChanges
           : this.gridSettingsFormGroup.get('mobileAutoFillHeight').valueChanges
        ).pipe(
          takeUntil(this.destroy$)
        ).subscribe(() => {
          this.updateGridSettingsFormState();
        });
        this.updateGridSettingsFormState();
      }
      if (this.layoutType === LayoutType.default && this.breakpointId !== 'default' || this.isScada()) {
        this.gridSettingsFormGroup.get('mobileAutoFillHeight').disable({emitEvent: false});
        this.gridSettingsFormGroup.get('mobileRowHeight').disable();
      }
    } else {
      this.gridSettingsFormGroup = this.fb.group({});
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  isScada() {
    return this.layoutType === LayoutType.scada;
  }

  get isDefaultLayout() {
    return this.layoutType === LayoutType.default;
  }

  get isDefaultBreakpoint(): boolean {
    return this.breakpointId === 'default';
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    let settings: DashboardSettings = null;
    let gridSettings: GridSettings = null;
    if (this.settings) {
      settings = {...this.settings, ...this.settingsFormGroup.getRawValue()};
    }
    if (this.gridSettings) {
      gridSettings = {...this.gridSettings, ...this.gridSettingsFormGroup.getRawValue()};
    }
    this.dialogRef.close({settings, gridSettings});
  }

  getStatesControllerTranslation(stateControllerId: string): string {
    if (this.stateControllerTranslationMap.has(stateControllerId)) {
      return this.translate.instant(this.stateControllerTranslationMap.get(stateControllerId));
    }
    return stateControllerId;
  }

  private updateGridSettingsFormState() {
    if (this.breakpointId === 'default') {
      const mobileAutoFillHeight: boolean = this.gridSettingsFormGroup.get('mobileAutoFillHeight').value;
      if (mobileAutoFillHeight) {
        this.gridSettingsFormGroup.get('mobileRowHeight').disable();
      } else {
        this.gridSettingsFormGroup.get('mobileRowHeight').enable();
      }
    } else {
      const autoFillHeight: boolean = this.gridSettingsFormGroup.get('autoFillHeight').value;
      const viewFormat: ViewFormatType = this.gridSettingsFormGroup.get('viewFormat').value;
      if (viewFormat !== ViewFormatType.list || autoFillHeight) {
        this.gridSettingsFormGroup.get('rowHeight').disable();
      } else {
        this.gridSettingsFormGroup.get('rowHeight').enable();
      }
      if (viewFormat === ViewFormatType.list) {
        this.gridSettingsFormGroup.get('columns').disable();
        this.gridSettingsFormGroup.get('minColumns').disable();
      } else {
        this.gridSettingsFormGroup.get('columns').enable();
        this.gridSettingsFormGroup.get('minColumns').enable();
      }
    }
  }
}
