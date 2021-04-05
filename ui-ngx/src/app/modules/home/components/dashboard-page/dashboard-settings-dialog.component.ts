///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { DashboardSettings, GridSettings, StateControllerId } from '@app/shared/models/dashboard.models';
import { isDefined, isUndefined } from '@core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { StatesControllerService } from './states/states-controller.service';

export interface DashboardSettingsDialogData {
  settings?: DashboardSettings;
  gridSettings?: GridSettings;
}


@Component({
  selector: 'tb-dashboard-settings-dialog',
  templateUrl: './dashboard-settings-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DashboardSettingsDialogComponent}],
  styleUrls: ['./dashboard-settings-dialog.component.scss']
})
export class DashboardSettingsDialogComponent extends DialogComponent<DashboardSettingsDialogComponent, DashboardSettingsDialogData>
  implements OnInit, ErrorStateMatcher {

  settings: DashboardSettings;
  gridSettings: GridSettings;
  logo_name;

  
  settingsFormGroup: FormGroup;
  gridSettingsFormGroup: FormGroup;

  stateControllerIds: string[];

  submitted = false;
  

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DashboardSettingsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DashboardSettingsDialogComponent, DashboardSettingsDialogData>,
              private fb: FormBuilder,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService,
              private statesControllerService: StatesControllerService) {
    super(store, router, dialogRef);

    this.stateControllerIds = Object.keys(this.statesControllerService.getStateControllers());

    this.settings = this.data.settings;
    this.gridSettings = this.data.gridSettings;
    this.logo_name = 'example_logo.png';

    if (this.settings) {
      this.settingsFormGroup = this.fb.group({
        stateControllerId: [isUndefined(this.settings.stateControllerId) ? 'entity' : this.settings.stateControllerId, []],
        toolbarAlwaysOpen: [isUndefined(this.settings.toolbarAlwaysOpen) ? true : this.settings.toolbarAlwaysOpen, []],
        showTitle: [isUndefined(this.settings.showTitle) ? true : this.settings.showTitle, []],
        titleColor: [isUndefined(this.settings.titleColor) ? 'rgba(0,0,0,0.870588)' : this.settings.titleColor, []],
        showDashboardsSelect: [isUndefined(this.settings.showDashboardsSelect) ? true : this.settings.showDashboardsSelect, []],
        showEntitiesSelect: [isUndefined(this.settings.showEntitiesSelect) ? true : this.settings.showEntitiesSelect, []],
        showFilters: [isUndefined(this.settings.showFilters) ? true : this.settings.showFilters, []],
        showDashboardLogo: [isUndefined(this.settings.showDashboardLogo) ? true : this.settings.showDashboardLogo, []],
        logoName: [isUndefined(this.settings.logoName) ? this.logo_name : this.settings.logoName, []],
        showDashboardTimewindow: [isUndefined(this.settings.showDashboardTimewindow) ? true : this.settings.showDashboardTimewindow, []],
        showDashboardExport: [isUndefined(this.settings.showDashboardExport) ? true : this.settings.showDashboardExport, []]
      });
      this.settingsFormGroup.get('stateControllerId').valueChanges.subscribe(
        (stateControllerId: StateControllerId) => {
          if (stateControllerId !== 'default') {
            this.settingsFormGroup.get('toolbarAlwaysOpen').setValue(true);
          }
        }
      );
    } else {
      this.settingsFormGroup = this.fb.group({});
    }

    if (this.gridSettings) {
      this.gridSettingsFormGroup = this.fb.group({
        color: [this.gridSettings.color || 'rgba(0,0,0,0.870588)', []],
        columns: [this.gridSettings.columns || 24, [Validators.required, Validators.min(10), Validators.max(1000)]],
        margin: [isDefined(this.gridSettings.margin) ? this.gridSettings.margin : 10,
          [Validators.required, Validators.min(0), Validators.max(50)]],
        autoFillHeight: [isUndefined(this.gridSettings.autoFillHeight) ? false : this.gridSettings.autoFillHeight, []],
        backgroundColor: [this.gridSettings.backgroundColor || 'rgba(0,0,0,0)', []],
        backgroundImageUrl: [this.gridSettings.backgroundImageUrl, []],
        backgroundSizeMode: [this.gridSettings.backgroundSizeMode || '100%', []],
        mobileAutoFillHeight: [isUndefined(this.gridSettings.mobileAutoFillHeight) ? false : this.gridSettings.mobileAutoFillHeight, []],
        mobileRowHeight: [isUndefined(this.gridSettings.mobileRowHeight) ? 70 : this.gridSettings.mobileRowHeight,
          [Validators.required, Validators.min(5), Validators.max(200)]]
      });
    } else {
      this.gridSettingsFormGroup = this.fb.group({});
    }
  }

  ngOnInit(): void {
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
      settings = {...this.settings, ...this.settingsFormGroup.value};
    }
    if (this.gridSettings) {
      gridSettings = {...this.gridSettings, ...this.gridSettingsFormGroup.value};
    }
    this.dialogRef.close({settings, gridSettings});
  }
}
