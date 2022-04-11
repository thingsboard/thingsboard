///
/// Copyright © 2016-2022 The Thingsboard Authors
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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { DashboardLayoutId, DashboardStateLayouts } from '@app/shared/models/dashboard.models';
import { deepClone, isDefined } from '@core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import {
  DashboardSettingsDialogComponent,
  DashboardSettingsDialogData
} from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import { LaouytType, LayoutWidthType } from "@home/components/dashboard-page/layout/layout.models";

export interface ManageDashboardLayoutsDialogData {
  layouts: DashboardStateLayouts;
}

@Component({
  selector: 'tb-manage-dashboard-layouts-dialog',
  templateUrl: './manage-dashboard-layouts-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardLayoutsDialogComponent}],
  styleUrls: ['./manage-dashboard-layouts-dialog.component.scss', '../../../components/dashboard/layout-button.scss']
})
export class ManageDashboardLayoutsDialogComponent extends DialogComponent<ManageDashboardLayoutsDialogComponent, DashboardStateLayouts>
  implements OnInit, ErrorStateMatcher {

  layoutsFormGroup: FormGroup;

  layouts: DashboardStateLayouts;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardLayoutsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDashboardLayoutsDialogComponent, DashboardStateLayouts>,
              private fb: FormBuilder,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    this.layouts = this.data.layouts;
    this.layoutsFormGroup = this.fb.group({
        main:  [{value: isDefined(this.layouts.main), disabled: true}, []],
        right: [isDefined(this.layouts.right), []],
        leftWidthPercentage: [50, [Validators.min(10), Validators.max(90)]],
        rightWidthPercentage: [50, [Validators.min(10), Validators.max(90)]],
        type: [LayoutWidthType.PERCENTAGE, []],
        fixedWidth: [150, [Validators.min(150), Validators.max(1700)]],
        fixedLayout: [LaouytType.MAIN, []]
      }
    );

    if (this.layouts.layoutDimension) {
      this.layoutsFormGroup.get('type').setValue(this.layouts.layoutDimension.type);
      if (this.layouts.layoutDimension.type === LayoutWidthType.FIXED) {
        this.layoutsFormGroup.get('fixedWidth').setValue(this.layouts.layoutDimension.fixedWidth);
        this.layoutsFormGroup.get('fixedLayout').setValue(this.layouts.layoutDimension.fixedLayout);
      } else {
        this.layoutsFormGroup.get('leftWidthPercentage').setValue(this.layouts.layoutDimension.leftWidthPercentage);
        this.layoutsFormGroup.get('rightWidthPercentage').setValue(100 - Number(this.layouts.layoutDimension.leftWidthPercentage));
      }
    }

    if (!this.layouts[LaouytType.MAIN]) {
      this.layouts[LaouytType.MAIN] = this.dashboardUtils.createDefaultLayoutData();
    }
    if (!this.layouts[LaouytType.RIGHT]) {
      this.layouts[LaouytType.RIGHT] = this.dashboardUtils.createDefaultLayoutData();
    }

    this.layoutsFormGroup.get('leftWidthPercentage').valueChanges.subscribe((value) => this.layoutControlChange('rightWidthPercentage', value));
    this.layoutsFormGroup.get('rightWidthPercentage').valueChanges.subscribe((value) => this.layoutControlChange('leftWidthPercentage', value));
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  openLayoutSettings(layoutId: DashboardLayoutId) {
    const gridSettings = deepClone(this.layouts[layoutId].gridSettings);
    this.dialog.open<DashboardSettingsDialogComponent, DashboardSettingsDialogData,
      DashboardSettingsDialogData>(DashboardSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings: null,
        gridSettings
      }
    }).afterClosed().subscribe((data) => {
      if (data && data.gridSettings) {
        this.dashboardUtils.updateLayoutSettings(this.layouts[layoutId], data.gridSettings);
        this.layoutsFormGroup.markAsDirty();
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    for (const l of Object.keys(this.layoutsFormGroup.controls)) {
      const control = this.layoutsFormGroup.controls[l];
      if (!control.value) {
        delete this.layouts[l];
      }
    }
    if (this.layoutsFormGroup.value.right) {
      const formValues = this.layoutsFormGroup.value;
      const widthType = formValues.type;
      (this.layouts.layoutDimension as any) = {
        type: widthType
      }
      if (widthType === LayoutWidthType.PERCENTAGE) {
        this.layouts.layoutDimension.leftWidthPercentage = formValues.leftWidthPercentage;
      } else {
        this.layouts.layoutDimension.fixedWidth = formValues.fixedWidth;
        this.layouts.layoutDimension.fixedLayout = formValues.fixedLayout;
      }
    }
    this.dialogRef.close(this.layouts);
  }

  buttonStyle(layout: DashboardLayoutId): { maxWidth: string } {
    if (this.layoutsFormGroup.value.type && this.layoutsFormGroup.value.right) {
      if (this.layoutsFormGroup.value.type !== LayoutWidthType.FIXED) {
        if (layout === LaouytType.MAIN) {
          return { maxWidth: this.layoutsFormGroup.value.leftWidthPercentage + "%" };
        } else {
          return { maxWidth: (100 - this.layoutsFormGroup.value.leftWidthPercentage) + "%" };
        }
      } else {
        return { maxWidth: '100%' };
      }
    }
  }

  formatSliderTooltipLabel(value: number):string {
    return `${value}|${100 - value}`;
  }

  layoutControlChange(key: string, value) {
    const previousValue = this.layoutsFormGroup.get(key).value;
    const valueToSet = 100 - Number(value);
    if (previousValue !== valueToSet) {
      this.layoutsFormGroup.get(key).setValue(100 - Number(value));
    }
  }
}
