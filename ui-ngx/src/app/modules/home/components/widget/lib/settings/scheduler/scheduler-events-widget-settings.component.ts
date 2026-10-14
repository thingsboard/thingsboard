// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import {
  AbstractControl,
  FormArray, FormControl,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CustomSchedulerEventType } from '@home/components/scheduler/scheduler-events.models';
import {
  customSchedulerEventTypeValidator
} from '@home/components/widget/lib/settings/scheduler/custom-scheduler-event-type.component';
import { buildPageStepSizeValues } from '@home/components/widget/lib/table-widget.models';
import { deepClone, isUndefined } from '@core/utils';

@Component({
    selector: 'tb-scheduler-events-widget-settings',
    templateUrl: './scheduler-events-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class SchedulerEventsWidgetSettingsComponent extends WidgetSettingsComponent {

  schedulerEventsWidgetSettingsForm: UntypedFormGroup;

  pageStepSizeValues = [];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.schedulerEventsWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      displayCreatedTime: true,
      displayType: true,
      displayCustomer: true,
      displaySchedule: false,
      displayPagination: true,
      defaultPageSize: 10,
      pageStepIncrement: null,
      pageStepCount: 3,
      defaultSortOrder: 'name',
      enabledViews: 'both',
      noDataDisplayMessage: '',
      forceDefaultEventType: '',
      customEventTypes: []
    };
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    settings.pageStepIncrement = settings.pageStepIncrement ?? settings.defaultPageSize;
    this.pageStepSizeValues = buildPageStepSizeValues(settings.pageStepCount, settings.pageStepIncrement);
    return settings;
  }

  protected prepareOutputSettings(settings: WidgetSettings): WidgetSettings {
    const config = deepClone(settings);
    this.setDisplayColumns(config);
    return config;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.schedulerEventsWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      displayColumns: [this.getDisplayColumns(settings)],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      pageStepCount: [settings.pageStepCount ?? 3, [Validators.min(1), Validators.max(100),
        Validators.required, Validators.pattern(/^\d*$/)]],
      pageStepIncrement: [settings.pageStepIncrement, [Validators.min(1), Validators.required, Validators.pattern(/^\d*$/)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      enabledViews: [settings.enabledViews, []],
      noDataDisplayMessage: [settings.noDataDisplayMessage, []],
      forceDefaultEventType: [settings.forceDefaultEventType, []],
      customEventTypes: this.prepareCustomEventTypesFormArray(settings.customEventTypes)
    });
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('customEventTypes', this.prepareCustomEventTypesFormArray(settings.customEventTypes), {emitEvent: false});
  }

  private prepareCustomEventTypesFormArray(customEventTypes: CustomSchedulerEventType[] | undefined): UntypedFormArray {
    const customEventTypesControls: Array<AbstractControl> = [];
    if (customEventTypes) {
      customEventTypes.forEach((customEventType) => {
        customEventTypesControls.push(this.fb.control(customEventType, [customSchedulerEventTypeValidator]));
      });
    }
    return this.fb.array(customEventTypesControls, []);
  }

  customEventTypesFormArray(): FormArray<FormControl & { new: boolean }> {
    return this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as FormArray<FormControl & { new: boolean }>;
  }

  public removeCustomEventType(index: number) {
    (this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as UntypedFormArray).removeAt(index);
  }

  public addCustomEventType() {
    const customEventType: CustomSchedulerEventType = {
      name: null,
      value: null,
      originator: null,
      msgType: null,
      metadata: null,
      template: null
    };
    const customEventTypesArray = this.schedulerEventsWidgetSettingsForm.get('customEventTypes') as UntypedFormArray;
    const customEventTypeControl = this.fb.control(customEventType, [customSchedulerEventTypeValidator]);
    (customEventTypeControl as any).new = true;
    customEventTypesArray.push(customEventTypeControl);
    this.schedulerEventsWidgetSettingsForm.updateValueAndValidity();
    if (!this.schedulerEventsWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.schedulerEventsWidgetSettingsForm.value);
    }
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination', 'pageStepCount', 'pageStepIncrement'];
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'pageStepCount' || trigger === 'pageStepIncrement') {
      this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').reset();
      this.pageStepSizeValues = buildPageStepSizeValues(this.schedulerEventsWidgetSettingsForm.get('pageStepCount').value,
        this.schedulerEventsWidgetSettingsForm.get('pageStepIncrement').value);
      return;
    }
    const displayPagination: boolean = this.schedulerEventsWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').enable({emitEvent});
      this.schedulerEventsWidgetSettingsForm.get('pageStepCount').enable({emitEvent: false});
      this.schedulerEventsWidgetSettingsForm.get('pageStepIncrement').enable({emitEvent: false});
    } else {
      this.schedulerEventsWidgetSettingsForm.get('defaultPageSize').disable({emitEvent});
      this.schedulerEventsWidgetSettingsForm.get('pageStepCount').disable({emitEvent: false});
      this.schedulerEventsWidgetSettingsForm.get('pageStepIncrement').disable({emitEvent: false});
    }
  }

  private getDisplayColumns(config: WidgetSettings): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.displayCreatedTime) || config.displayCreatedTime) {
      buttons.push('displayCreatedTime');
    }
    if (isUndefined(config.displayType) || config.displayType) {
      buttons.push('displayType');
    }
    if (isUndefined(config.displayCustomer) || config.displayCustomer) {
      buttons.push('displayCustomer');
    }
    if (isUndefined(config.displaySchedule) || config.displaySchedule) {
      buttons.push('displaySchedule');
    }
    return buttons;
  }

  private setDisplayColumns(config: WidgetSettings) {
    const buttons = config.displayColumns
    config.displayCreatedTime = buttons.includes('displayCreatedTime');
    config.displayType = buttons.includes('displayType');
    config.displayCustomer = buttons.includes('displayCustomer');
    config.displaySchedule = buttons.includes('displaySchedule');
    delete config.displayColumns;
  }
}
