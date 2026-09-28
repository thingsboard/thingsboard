// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { buildPageStepSizeValues } from '@home/components/widget/lib/table-widget.models';

@Component({
    selector: 'tb-blob-entities-widget-settings',
    templateUrl: './blob-entities-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class BlobEntitiesWidgetSettingsComponent extends WidgetSettingsComponent {

  blobEntitiesWidgetSettingsForm: UntypedFormGroup;

  pageStepSizeValues = [];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.blobEntitiesWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      displayCreatedTime: true,
      displayType: true,
      displayCustomer: true,
      displayPagination: true,
      defaultPageSize: 10,
      pageStepIncrement: null,
      pageStepCount: 3,
      defaultSortOrder: 'name',
      noDataDisplayMessage: '',
      forceDefaultType: ''
    };
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    settings.pageStepIncrement = settings.pageStepIncrement ?? settings.defaultPageSize;
    this.pageStepSizeValues = buildPageStepSizeValues(settings.pageStepCount, settings.pageStepIncrement);
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.blobEntitiesWidgetSettingsForm = this.fb.group({
      title: [settings.title, []],
      displayCreatedTime: [settings.displayCreatedTime, []],
      displayType: [settings.displayType, []],
      displayCustomer: [settings.displayCustomer, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      pageStepCount: [settings.pageStepCount ?? 3, [Validators.min(1), Validators.max(100),
        Validators.required, Validators.pattern(/^\d*$/)]],
      pageStepIncrement: [settings.pageStepIncrement, [Validators.min(1), Validators.required, Validators.pattern(/^\d*$/)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      noDataDisplayMessage: [settings.noDataDisplayMessage, []],
      forceDefaultType: [settings.forceDefaultType, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination', 'pageStepCount', 'pageStepIncrement'];
  }

  protected updateValidators(emitEvent: boolean, trigger: string) {
    if (trigger === 'pageStepCount' || trigger === 'pageStepIncrement') {
      this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').reset();
      this.pageStepSizeValues = buildPageStepSizeValues(this.blobEntitiesWidgetSettingsForm.get('pageStepCount').value,
        this.blobEntitiesWidgetSettingsForm.get('pageStepIncrement').value);
      return;
    }
    const displayPagination: boolean = this.blobEntitiesWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').enable({emitEvent});
      this.blobEntitiesWidgetSettingsForm.get('pageStepCount').enable({emitEvent: false});
      this.blobEntitiesWidgetSettingsForm.get('pageStepIncrement').enable({emitEvent: false});
    } else {
      this.blobEntitiesWidgetSettingsForm.get('defaultPageSize').disable({emitEvent});
      this.blobEntitiesWidgetSettingsForm.get('pageStepCount').disable({emitEvent: false});
      this.blobEntitiesWidgetSettingsForm.get('pageStepIncrement').disable({emitEvent: false});
    }
  }
}
