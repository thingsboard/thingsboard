// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GpioItem, gpioItemValidator } from '@home/components/widget/lib/settings/gpio/gpio-item.component';

@Component({
    selector: 'tb-gpio-panel-widget-settings',
    templateUrl: './gpio-panel-widget-settings.component.html',
    styleUrls: ['./../widget-settings.scss'],
    standalone: false
})
export class GpioPanelWidgetSettingsComponent extends WidgetSettingsComponent {

  gpioPanelWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.gpioPanelWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ledPanelBackgroundColor: '#008a00',
      gpioList: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gpioPanelWidgetSettingsForm = this.fb.group({

      // Panel settings

      ledPanelBackgroundColor: [settings.ledPanelBackgroundColor, [Validators.required]],

      // --> GPIO leds

      gpioList: this.prepareGpioListFormArray(settings.gpioList),

    });
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('gpioList', this.prepareGpioListFormArray(settings.gpioList), {emitEvent: false});
  }

  private prepareGpioListFormArray(gpioList: GpioItem[] | undefined): UntypedFormArray {
    const gpioListControls: Array<AbstractControl> = [];
    if (gpioList) {
      gpioList.forEach((gpioItem) => {
        gpioListControls.push(this.fb.control(gpioItem, [gpioItemValidator(true)]));
      });
    }
    return this.fb.array(gpioListControls, [(control: AbstractControl) => {
      const gpioItems = control.value;
      if (!gpioItems || !gpioItems.length) {
        return {
          gpioItems: true
        };
      }
      return null;
    }]);
  }

  gpioListFormArray(): UntypedFormArray {
    return this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray;
  }

  public trackByGpioItem(index: number, gpioItemControl: AbstractControl): any {
    return gpioItemControl;
  }

  public removeGpioItem(index: number) {
    (this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray).removeAt(index);
  }

  public addGpioItem() {
    const gpioItem: GpioItem = {
      pin: null,
      label: null,
      row: null,
      col: null,
      color: null
    };
    const gpioListArray = this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray;
    const gpioItemControl = this.fb.control(gpioItem, [gpioItemValidator(true)]);
    (gpioItemControl as any).new = true;
    gpioListArray.push(gpioItemControl);
    this.gpioPanelWidgetSettingsForm.updateValueAndValidity();
    if (!this.gpioPanelWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.gpioPanelWidgetSettingsForm.value);
    }
  }
}
