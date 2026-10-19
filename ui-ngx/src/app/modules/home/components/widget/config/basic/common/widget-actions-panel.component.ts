// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetActionsData } from '@home/components/widget/action/manage-widget-actions.component.models';
import { WidgetActionDescriptor } from '@shared/models/widget.models';
import {
  ManageWidgetActionsDialogComponent,
  ManageWidgetActionsDialogData
} from '@home/components/widget/action/manage-widget-actions-dialog.component';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-widget-actions-panel',
    templateUrl: './widget-actions-panel.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => WidgetActionsPanelComponent),
            multi: true
        }
    ],
    standalone: false
})
export class WidgetActionsPanelComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  strokedPanel = false;

  actionsFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private widgetConfigComponent: WidgetConfigComponent,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.actionsFormGroup = this.fb.group({
      actions: [null, []]
    });
    this.actionsFormGroup.get('actions').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (val) => this.propagateChange(val)
    );
  }

  writeValue(actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>}): void {
    this.actionsFormGroup.get('actions').patchValue(actions || {}, {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.actionsFormGroup.disable({emitEvent: false});
    } else {
      this.actionsFormGroup.enable({emitEvent: false});
    }
  }

  public get widgetActionSourceIds(): Array<string> {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    return actions ? Object.keys(actions) : [];
  }

  public widgetActionsByActionSourceId(actionSourceId: string): Array<WidgetActionDescriptor> {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    return actions[actionSourceId] || [];
  }

  public get hasWidgetActions(): boolean {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    if (actions) {
      for (const actionSourceId of Object.keys(actions)) {
        if (actions[actionSourceId] && actions[actionSourceId].length) {
          return true;
        }
      }
    }
    return false;
  }

  public manageWidgetActions() {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    const actionsData: WidgetActionsData = {
      actionsMap: deepClone(actions),
      actionSources: this.widgetConfigComponent.modelValue.actionSources || {}
    };
    this.dialog.open<ManageWidgetActionsDialogComponent, ManageWidgetActionsDialogData,
      {[actionSourceId: string]: Array<WidgetActionDescriptor>}>(ManageWidgetActionsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        widgetTitle: this.widgetConfigComponent.modelValue.widgetName,
        callbacks: this.widgetConfigComponent.widgetConfigCallbacks,
        actionsData,
        widgetType: this.widgetConfigComponent.widgetType,
        defaultIconColor: this.widgetConfigComponent.widgetSettings.get('color').value,
        additionalWidgetActionTypes: this.widgetConfigComponent.modelValue.typeParameters.additionalWidgetActionTypes
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.actionsFormGroup.get('actions').patchValue(res);
          this.cd.markForCheck();
        }
      }
    );
  }

}
