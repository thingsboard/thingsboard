// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
    selector: 'tb-device-claiming-widget-settings',
    templateUrl: './device-claiming-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class DeviceClaimingWidgetSettingsComponent extends WidgetSettingsComponent {

  deviceClaimingWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.deviceClaimingWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      labelClaimButon: '',
      deviceSecret: false,
      showLabel: true,
      deviceLabel: '',
      secretKeyLabel: '',
      successfulClaimDevice: '',
      deviceNotFound: '',
      failedClaimDevice: '',
      requiredErrorDevice: '',
      requiredErrorSecretKey: '',
      relateDevice: false,
      relateDirection: 'from',
      relateType: 'Contains'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.deviceClaimingWidgetSettingsForm = this.fb.group({

      // General settings

      widgetTitle: [settings.widgetTitle, []],
      labelClaimButon: [settings.labelClaimButon, []],
      deviceSecret: [settings.deviceSecret, []],

      // Labels settings

      showLabel: [settings.showLabel, []],
      deviceLabel: [settings.deviceLabel, []],
      secretKeyLabel: [settings.secretKeyLabel, []],

      // Message settings

      successfulClaimDevice: [settings.successfulClaimDevice, []],
      deviceNotFound: [settings.deviceNotFound, []],
      failedClaimDevice: [settings.failedClaimDevice, []],
      requiredErrorDevice: [settings.requiredErrorDevice, []],
      requiredErrorSecretKey: [settings.requiredErrorSecretKey, []],

      // Relations settings

      relateDevice: [settings.relateDevice, []],
      relateDirection: [settings.relateDirection, []],
      relateType: [settings.relateType, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['deviceSecret', 'showLabel', 'relateDevice'];
  }

  protected updateValidators(emitEvent: boolean) {
    const deviceSecret: boolean = this.deviceClaimingWidgetSettingsForm.get('deviceSecret').value;
    const showLabel: boolean = this.deviceClaimingWidgetSettingsForm.get('showLabel').value;
    const relateDevice: boolean = this.deviceClaimingWidgetSettingsForm.get('relateDevice').value;
    if (deviceSecret) {
      if (showLabel) {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').enable();
      } else {
        this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
      }
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').disable();
      this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').disable();
    }
    if (showLabel) {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('deviceLabel').disable();
    }
    if (relateDevice) {
      this.deviceClaimingWidgetSettingsForm.get('relateDirection').enable();
      this.deviceClaimingWidgetSettingsForm.get('relateType').enable();
    } else {
      this.deviceClaimingWidgetSettingsForm.get('relateDirection').disable();
      this.deviceClaimingWidgetSettingsForm.get('relateType').disable();
    }
    this.deviceClaimingWidgetSettingsForm.get('secretKeyLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('deviceLabel').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('requiredErrorSecretKey').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('relateDirection').updateValueAndValidity({emitEvent});
    this.deviceClaimingWidgetSettingsForm.get('relateType').updateValueAndValidity({emitEvent});
  }

}
