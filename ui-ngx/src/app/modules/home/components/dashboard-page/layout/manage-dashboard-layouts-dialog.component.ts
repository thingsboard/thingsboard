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

import { Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AbstractControl,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  FormGroupDirective,
  NgForm,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import {
  BreakpointInfo,
  DashboardLayout,
  DashboardLayoutId,
  DashboardStateLayouts,
  LayoutDimension, LayoutType, layoutTypes,
  layoutTypeTranslationMap
} from '@app/shared/models/dashboard.models';
import { deepClone, isDefined, isEqual } from '@core/utils';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import {
  DashboardSettingsDialogComponent,
  DashboardSettingsDialogData
} from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import {
  LayoutFixedSize,
  LayoutPercentageSize,
  LayoutWidthType
} from '@home/components/dashboard-page/layout/layout.models';
import { Subscription } from 'rxjs';
import { MatTooltip } from '@angular/material/tooltip';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';

export interface ManageDashboardLayoutsDialogData {
  layouts: DashboardStateLayouts;
}

export interface DashboardLayoutSettings {
  icon: string;
  name: string;
  descriptionSize?: string;
  layout: DashboardLayout;
  breakpoint: string;
}

@Component({
  selector: 'tb-manage-dashboard-layouts-dialog',
  templateUrl: './manage-dashboard-layouts-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardLayoutsDialogComponent}],
  styleUrls: ['./manage-dashboard-layouts-dialog.component.scss', '../../../components/dashboard/layout-button.scss']
})
export class ManageDashboardLayoutsDialogComponent extends DialogComponent<ManageDashboardLayoutsDialogComponent, DashboardStateLayouts>
  implements ErrorStateMatcher, OnDestroy {

  @ViewChild('tooltip') tooltip: MatTooltip;

  layoutsFormGroup: UntypedFormGroup;
  addBreakpointFormGroup: UntypedFormGroup;

  layoutWidthType = LayoutWidthType;

  layoutPercentageSize = LayoutPercentageSize;

  layoutFixedSize = LayoutFixedSize;

  layoutTypes = layoutTypes;
  layoutTypeTranslations = layoutTypeTranslationMap;

  dataSource: DashboardLayoutDatasource;

  addBreakpointMode = false;

  private layoutBreakpoints: DashboardLayoutSettings[] = [];
  private readonly layouts: DashboardStateLayouts;

  private subscriptions: Array<Subscription> = [];

  private submitted = false;

  breakpoints: BreakpointInfo[];
  breakpointsData: {[breakpointId in string]: BreakpointInfo} = {};

  allowBreakpointIds = [];
  selectedBreakpointIds = ['default'];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: ManageDashboardLayoutsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              protected dialogRef: MatDialogRef<ManageDashboardLayoutsDialogComponent, DashboardStateLayouts>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              private dashboardUtils: DashboardUtilsService,
              private translate: TranslateService,
              private dialog: MatDialog,) {
    super(store, router, dialogRef);

    this.layouts = this.data.layouts;
    this.dataSource = new DashboardLayoutDatasource();

    this.breakpoints = this.dashboardUtils.getListBreakpoint();
    this.breakpoints.forEach((breakpoint) => {
      this.breakpointsData[breakpoint.id] = breakpoint;
    });

    let layoutType = LayoutType.default;
    if (isDefined(this.layouts.right)) {
      layoutType = LayoutType.divider;
    } else if (isDefined(this.layouts.main.gridSettings.layoutType)) {
      layoutType = this.layouts.main.gridSettings.layoutType;
    }

    this.layoutsFormGroup = this.fb.group({
        layoutType: [layoutType],
        main: [{value: isDefined(this.layouts.main), disabled: true}],
        sliderPercentage: [50],
        sliderFixed: [this.layoutFixedSize.MIN],
        leftWidthPercentage: [50,
          [Validators.min(this.layoutPercentageSize.MIN), Validators.max(this.layoutPercentageSize.MAX), Validators.required]],
        rightWidthPercentage: [50,
          [Validators.min(this.layoutPercentageSize.MIN), Validators.max(this.layoutPercentageSize.MAX), Validators.required]],
        type: [LayoutWidthType.PERCENTAGE],
        fixedWidth: [this.layoutFixedSize.MIN,
          [Validators.min(this.layoutFixedSize.MIN), Validators.max(this.layoutFixedSize.MAX), Validators.required]],
        fixedLayout: ['main', []]
      }
    );

    this.addBreakpointFormGroup = this.fb.group({
      new: [],
      copyFrom: []
    });

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
            sliderFixed: this.layouts.main.gridSettings.layoutDimension.fixedWidth
          }, {emitEvent: false});
        } else {
          const leftWidthPercentage = Number(this.layouts.main.gridSettings.layoutDimension.leftWidthPercentage);
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

    this.addLayoutConfiguration('default');

    if (!this.isDividerLayout && this.layouts.main.breakpoints) {
      for (const breakpoint of Object.keys(this.layouts.main.breakpoints)) {
        this.addLayoutConfiguration(breakpoint);
        this.selectedBreakpointIds.push(breakpoint);
      }
    }

    this.allowBreakpointIds = Object.keys(this.breakpointsData)
      .filter((item) => !this.selectedBreakpointIds.includes(item));

    this.dataSource.loadData(this.layoutBreakpoints);

    this.subscriptions.push(
      this.layoutsFormGroup.get('sliderPercentage').valueChanges
        .subscribe(
          (value) => this.layoutsFormGroup.get('leftWidthPercentage').patchValue(value)
        ));
    this.subscriptions.push(
      this.layoutsFormGroup.get('sliderFixed').valueChanges
        .subscribe(
          (value) => {
            this.layoutsFormGroup.get('fixedWidth').patchValue(value);
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
            this.showTooltip(this.layoutsFormGroup.get('fixedWidth'), LayoutWidthType.FIXED,
              this.layoutsFormGroup.get('fixedLayout').value);
            this.layoutsFormGroup.get('sliderFixed').setValue(value, {emitEvent: false});
          }
        ));
  }

  ngOnDestroy(): void {
    for (const subscription of this.subscriptions) {
      subscription.unsubscribe();
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  openLayoutSettings(layoutId: DashboardLayoutId, breakpoint?: string) {
    let gridSettings;
    if (isDefined(breakpoint) && breakpoint !== 'default') {
      gridSettings = deepClone(this.layouts[layoutId].breakpoints[breakpoint].gridSettings);
    } else {
      gridSettings = deepClone(this.layouts[layoutId].gridSettings);
    }
    this.dialog.open<DashboardSettingsDialogComponent, DashboardSettingsDialogData,
      DashboardSettingsDialogData>(DashboardSettingsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        settings: null,
        gridSettings,
        isRightLayout: this.isDividerLayout && layoutId === 'right'
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
    const layoutType = this.layoutsFormGroup.value.layoutType;
    this.layouts.main.gridSettings.layoutType = layoutType;
    if (!this.isDividerLayout) {
      delete this.layouts.right;
      for (const breakpoint of Object.values(this.layouts.main.breakpoints)) {
        breakpoint.gridSettings.layoutType = layoutType;
      }
    } else {
      delete this.layouts.main.breakpoints;
      this.layouts.right.gridSettings.layoutType = layoutType;
    }
    delete this.layouts.main.gridSettings.layoutDimension;
    if (this.layouts.right?.gridSettings) {
      delete this.layouts.right.gridSettings.layoutDimension;
    }
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
    if (this.isDividerLayout) {
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
    if (this.layoutsFormGroup.get('type').value === LayoutWidthType.FIXED && this.isDividerLayout) {
      this.layoutsFormGroup.get('fixedLayout').setValue(layout);
      this.layoutsFormGroup.get('fixedLayout').markAsDirty();
    }
  }

  private showTooltip(control: AbstractControl, layoutType: LayoutWidthType, layoutSide: DashboardLayoutId): void {
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
        message = this.translate.instant('layout.value-min-error', {min: control.errors.min.min, unit});
      } else if (control.errors.max) {
        message = this.translate.instant('layout.value-max-error', {max: control.errors.max.max, unit});
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

  layoutButtonClass(side: DashboardLayoutId, border: boolean = false): string {
    const formValues = this.layoutsFormGroup.value;
    if (this.isDividerLayout) {
      let classString = border ? 'tb-layout-button-main ' : '';
      if (!(formValues.fixedLayout === side || formValues.type === LayoutWidthType.PERCENTAGE)) {
        classString += 'tb-fixed-layout-button';
      }
      return classString;
    }
  }

  layoutButtonText(side: DashboardLayoutId): string {
    const formValues = this.layoutsFormGroup.value;
    if (!(formValues.fixedLayout === side || !this.isDividerLayout || formValues.type === LayoutWidthType.PERCENTAGE)) {
      if (side === 'main') {
        return this.translate.instant('layout.left-side');
      } else {
        return this.translate.instant('layout.right-side');
      }
    }
  }

  showPreviewInputs(side: DashboardLayoutId): boolean {
    const formValues = this.layoutsFormGroup.value;
    return this.isDividerLayout && (formValues.type === LayoutWidthType.PERCENTAGE || formValues.fixedLayout === side);
  }

  get isDividerLayout(): boolean {
    return this.layoutsFormGroup.get('layoutType').value === LayoutType.divider;
  }

  deleteBreakpoint(breakpointId: string): void {
    delete this.layouts.main.breakpoints[breakpointId];
    if (isEqual(this.layouts.main.breakpoints, {})) {
      delete this.layouts.main.breakpoints;
    }
    this.layoutBreakpoints = this.layoutBreakpoints.filter((item) => item.breakpoint !== breakpointId);
    this.allowBreakpointIds.push(breakpointId);
    this.selectedBreakpointIds = this.selectedBreakpointIds.filter((item) => item !== breakpointId);
    this.dataSource.loadData(this.layoutBreakpoints);
    this.layoutsFormGroup.markAsDirty();
  }

  addBreakpoint() {
    this.addBreakpointMode = !this.addBreakpointMode;
    if (this.addBreakpointMode) {
      this.addBreakpointFormGroup.reset({
        new: this.allowBreakpointIds[0],
        copyFrom: 'default'
      });
    }
  }

  createdBreakPoint() {
    const layoutConfig = this.layouts.main;
    const newBreakpoint = this.addBreakpointFormGroup.value.new;
    const sourceBreakpoint = this.addBreakpointFormGroup.value.copyFrom;
    const sourceLayout = sourceBreakpoint === 'default' ? layoutConfig : layoutConfig.breakpoints[sourceBreakpoint];

    if (!layoutConfig.breakpoints) {
      layoutConfig.breakpoints = {};
    }

    layoutConfig.breakpoints[newBreakpoint] = {
      gridSettings: deepClone(sourceLayout.gridSettings),
      widgets: deepClone(sourceLayout.widgets),
    };
    this.selectedBreakpointIds.push(newBreakpoint);
    this.allowBreakpointIds = this.allowBreakpointIds.filter((item) => item !== newBreakpoint);
    this.addLayoutConfiguration(newBreakpoint);

    this.dataSource.loadData(this.layoutBreakpoints);

    this.addBreakpointMode = false;

    this.layoutsFormGroup.markAsDirty();
  }

  private addLayoutConfiguration(breakpointId: string) {
    const layout = breakpointId === 'default' ? this.layouts.main : this.layouts.main.breakpoints[breakpointId];
    const size = breakpointId === 'default' ? '' : this.createDescriptionSize(breakpointId);
    this.layoutBreakpoints.push({
      icon: 'mdi:monitor',
      name: breakpointId,
      layout,
      descriptionSize: size,
      breakpoint: breakpointId
    });
  }

  private createDescriptionSize(breakpointId: string): string {
    const currentData = this.breakpointsData[breakpointId];
    const minStr = isDefined(currentData.minWidth) ? `min-width: ${currentData.minWidth}px` : '';
    const maxStr = isDefined(currentData.maxWidth) ? `min-width: ${currentData.maxWidth}px` : '';
    return minStr && maxStr ? `${minStr} and ${maxStr}` : `${minStr}${maxStr}`;
  }
}

export class DashboardLayoutDatasource extends TbTableDatasource<DashboardLayoutSettings> {
  constructor() {
    super();
  }

  connect() {
    if (this.dataSubject.isStopped) {
      this.dataSubject.isStopped = false;
    }
    return this.dataSubject.asObservable();
  }
}
