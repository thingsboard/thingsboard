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

import { Component, ElementRef, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { DashboardLayoutId, DashboardStateLayouts, LayoutDimension } from '@app/shared/models/dashboard.models';
import { deepClone, isDefined } from '@core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import {
  DashboardSettingsDialogComponent,
  DashboardSettingsDialogData
} from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import { LayoutWidthType } from '@home/components/dashboard-page/layout/layout.models';
import { Subscription } from 'rxjs';
import { MatTooltip } from "@angular/material/tooltip";

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
  implements ErrorStateMatcher {

  @ViewChild('tooltip') tooltip: MatTooltip;

  layoutsFormGroup: FormGroup;

  layouts: DashboardStateLayouts;

  LayoutWidthType = LayoutWidthType;

  subscriptions: Array<Subscription> = [];

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
              private dialog: MatDialog,
              private elementRef: ElementRef) {
    super(store, router, dialogRef);

    this.layouts = this.data.layouts;

    this.layoutsFormGroup = this.fb.group({
        main: [{value: isDefined(this.layouts.main), disabled: true}],
        right: [isDefined(this.layouts.right)],
        sliderPercentage: [50],
        sliderFixed: [150],
        leftWidthPercentage: [50, [Validators.min(10), Validators.max(90), Validators.required]],
        rightWidthPercentage: [50, [Validators.min(10), Validators.max(90), Validators.required]],
        type: [LayoutWidthType.PERCENTAGE],
        fixedWidth: [150, [Validators.min(150), Validators.max(1700), Validators.required]],
        fixedLayout: ['main', []]
      }
    );

    this.subscriptions.push(
      this.layoutsFormGroup.get('type').valueChanges.subscribe(
        (value) => {
          if (value === LayoutWidthType.FIXED) {
            this.layoutsFormGroup.get('leftWidthPercentage').disable();
            this.layoutsFormGroup.get('rightWidthPercentage').disable();
            this.layoutsFormGroup.get('fixedWidth').enable();
            this.layoutsFormGroup.get('fixedLayout').enable();
          } else {
            this.layoutsFormGroup.get('leftWidthPercentage').enable();
            this.layoutsFormGroup.get('rightWidthPercentage').enable();
            this.layoutsFormGroup.get('fixedWidth').disable();
            this.layoutsFormGroup.get('fixedLayout').disable();
          }
        }
      )
    );

    if (this.layouts.right) {
      if (this.layouts.right.gridSettings.layoutDimension) {
        this.layoutsFormGroup.patchValue({
          fixedLayout: this.layouts.right.gridSettings.layoutDimension.fixedLayout,
          type: LayoutWidthType.FIXED,
          fixedWidth: this.layouts.right.gridSettings.layoutDimension.fixedWidth,
          sliderFixed: this.layouts.right.gridSettings.layoutDimension.fixedWidth
        }, {emitEvent: false});
      } else if (this.layouts.main.gridSettings.layoutDimension) {
        if (this.layouts.main.gridSettings.layoutDimension.type === LayoutWidthType.FIXED) {
          this.layoutsFormGroup.patchValue({
            fixedLayout: this.layouts.main.gridSettings.layoutDimension.fixedLayout,
            type: LayoutWidthType.FIXED,
            fixedWidth: this.layouts.main.gridSettings.layoutDimension.fixedWidth,
            sliderFixed: this.layouts.right.gridSettings.layoutDimension.fixedWidth
          }, {emitEvent: false});
        } else {
          const leftWidthPercentage: number = Number(this.layouts.main.gridSettings.layoutDimension.leftWidthPercentage);
          this.layoutsFormGroup.patchValue({
            leftWidthPercentage,
            sliderPercentage: leftWidthPercentage,
            rightWidthPercentage: 100 - Number(leftWidthPercentage)
          }, {emitEvent: false});
        }
      }
    }

    if (!this.layouts.main) {
      this.layouts.main = this.dashboardUtils.createDefaultLayoutData();
    }
    if (!this.layouts.right) {
      this.layouts.right = this.dashboardUtils.createDefaultLayoutData();
    }

    this.subscriptions.push(
      this.layoutsFormGroup.get('sliderPercentage').valueChanges
        .subscribe(
          (value) => this.layoutsFormGroup.get('leftWidthPercentage').patchValue(value)
        ));
    this.subscriptions.push(
      this.layoutsFormGroup.get('sliderFixed').valueChanges
        .subscribe(
          (value) => {
            this.layoutsFormGroup.get('fixedWidth').patchValue(value)
          }
        ));
    this.subscriptions.push(
      this.layoutsFormGroup.get('leftWidthPercentage').valueChanges
        .subscribe(
          (value) => {
            this.showTooltip(this.layoutsFormGroup.get('leftWidthPercentage'), LayoutWidthType.PERCENTAGE, 'main');
            this.layoutControlChange('rightWidthPercentage', value);
          }
        ));
    this.subscriptions.push(
      this.layoutsFormGroup.get('rightWidthPercentage').valueChanges
        .subscribe(
          (value) => {
            this.showTooltip(this.layoutsFormGroup.get('rightWidthPercentage'), LayoutWidthType.PERCENTAGE, 'right');
            this.layoutControlChange('leftWidthPercentage', value);
          }
        ));
    this.subscriptions.push(
      this.layoutsFormGroup.get('fixedWidth').valueChanges
        .subscribe(
          (value) => {
            this.showTooltip(this.layoutsFormGroup.get('fixedWidth'), LayoutWidthType.FIXED, this.layoutsFormGroup.get('fixedLayout').value);
            this.layoutsFormGroup.get('sliderFixed').setValue(value, {emitEvent: false});
          }
        ));
  }

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
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
    const layouts = ['main', 'right'];
    for (const l of layouts) {
      const control = this.layoutsFormGroup.controls[l];
      if (!control.value) {
        if (this.layouts[l]) {
          delete this.layouts[l];
        }
      }
    }
    delete this.layouts.main.gridSettings.layoutDimension;
    delete this.layouts.right.gridSettings.layoutDimension;
    if (this.layoutsFormGroup.value.right) {
      const formValues = this.layoutsFormGroup.value;
      const widthType = formValues.type;
      const layoutDimension: LayoutDimension = {
        type: widthType
      };
      if (widthType === LayoutWidthType.PERCENTAGE) {
        layoutDimension.leftWidthPercentage = formValues.leftWidthPercentage;
        this.layouts.main.gridSettings.layoutDimension = layoutDimension;
      } else {
        layoutDimension.fixedWidth = formValues.fixedWidth;
        layoutDimension.fixedLayout = formValues.fixedLayout;
        if (formValues.fixedLayout === 'main') {
          this.layouts.main.gridSettings.layoutDimension = layoutDimension;
        } else {
          this.layouts.right.gridSettings.layoutDimension = layoutDimension;
        }
      }
    }
    this.dialogRef.close(this.layouts);
  }

  buttonFlexValue(): number {
    const formValues = this.layoutsFormGroup.value;
    if (formValues.right) {
      if (formValues.type !== LayoutWidthType.FIXED) {
        return formValues.leftWidthPercentage;
      } else {
        if (formValues.fixedLayout === 'main') {
          return 10;
        } else {
          return 90;
        }
      }
    }
  }

  formatSliderTooltipLabel(value: number): string | number {
    return this.layoutsFormGroup.get('type').value === LayoutWidthType.FIXED ? value : `${value}|${100 - value}`;
  }

  private layoutControlChange(key: string, value) {
    const valueToSet = 100 - Number(value);
    this.layoutsFormGroup.get(key).setValue(valueToSet, {emitEvent: false});
    this.layoutsFormGroup.get('sliderPercentage')
      .setValue(key === 'leftWidthPercentage' ? valueToSet : Number(value), {emitEvent: false});
  }

  setFixedLayout(layout: string): void {
    const layoutButtons = this.elementRef.nativeElement.querySelectorAll('.tb-layout-button');
    if (layoutButtons?.length) {
      let elementToDisable: HTMLButtonElement;
      if (layout === 'right') {
        elementToDisable = layoutButtons[0];
      } else {
        elementToDisable = layoutButtons[1];
      }

      elementToDisable.disabled = true;
      setTimeout(() => {
        elementToDisable.disabled = false;
      }, 250);
    }

    if (this.layoutsFormGroup.get('type').value === LayoutWidthType.FIXED) {
      this.layoutsFormGroup.get('fixedLayout').setValue(layout);
    }
  }

  private showTooltip(control: AbstractControl, layoutType: LayoutWidthType, layoutSide: string): void {
    if (control.errors) {
      let message: string;
      const unit = layoutType === LayoutWidthType.FIXED ? 'px' : '%';

      if (control.errors.required) {
        if (layoutType === LayoutWidthType.FIXED) {
          message = this.translate.instant('layout.layout-fixed-width-required');
        } else {
          if (layoutSide === 'right') {
            message = this.translate.instant('layout.right-width-percentage-required');
          } else {
            message = this.translate.instant('layout.left-width-percentage-required');
          }
        }
      } else if (control.errors.min) {
        message = this.translate.instant('layout.value-min-error', {min: control.errors.min.min, unit: unit});
      } else if (control.errors.max) {
        message = this.translate.instant('layout.value-max-error', {max: control.errors.max.max, unit: unit});
      }

      if (layoutSide === 'main') {
        this.tooltip.tooltipClass = 'tb-layout-error-tooltip-main';
      } else {
        this.tooltip.tooltipClass = 'tb-layout-error-tooltip-right';
      }

      this.tooltip.message = message;
      this.tooltip.show(1300);
    } else {
      this.tooltip.message = '';
      this.tooltip.hide();
    }
  }

  layoutButtonTextAndClass(side: string, isText: boolean): string {
    const formValues = this.layoutsFormGroup.value;
    if (!(formValues.fixedLayout === side || !formValues.right || formValues.type === LayoutWidthType.PERCENTAGE)) {
      if (isText) {
        if (side === 'main') {
          return this.translate.instant('layout.left-side');
        } else {
          return this.translate.instant('layout.right-side');
        }
      } else {
        return 'tb-fixed-layout-button';
      }
    }
  }

  showPreviewInputs(side: string): boolean {
    const formValues = this.layoutsFormGroup.value;
    return formValues.right &&
      (
        formValues.type === LayoutWidthType.PERCENTAGE ||
        (formValues.fixedLayout === side && formValues.type === LayoutWidthType.FIXED)
      );
  }
}
