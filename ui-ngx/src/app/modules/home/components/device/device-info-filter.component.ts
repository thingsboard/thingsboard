///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Inject,
  InjectionToken,
  Input,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { DeviceInfoFilter } from '@shared/models/device.models';
import { deepClone, isDefinedAndNotNull, isEmpty, isUndefinedOrNull } from '@core/utils';
import { EntityInfoData } from '@shared/models/entity.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent, Subscription } from 'rxjs';

export const DEVICE_FILTER_CONFIG_DATA = new InjectionToken<any>('DeviceFilterConfigData');

export interface DeviceFilterConfigData {
  panelMode: boolean;
  deviceInfoFilter: DeviceInfoFilter;
  initialFilterConfig?: DeviceInfoFilter;
}

// @dynamic
@Component({
  selector: 'tb-device-info-filter',
  templateUrl: './device-info-filter.component.html',
  styleUrls: ['./device-info-filter.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeviceInfoFilterComponent),
      multi: true
    }
  ]
})
export class DeviceInfoFilterComponent implements OnInit, ControlValueAccessor {

  @ViewChild('deviceFilterPanel')
  deviceFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @Input()
  initialDeviceFilterConfig: DeviceInfoFilter = {
    deviceProfileId: null,
    active: null
  };

  panelMode = false;

  buttonDisplayValue = this.translate.instant('device.device-filter-title');

  deviceInfoFilterForm: FormGroup;

  deviceFilterOverlayRef: OverlayRef;

  panelResult: DeviceInfoFilter = null;

  private deviceProfileInfo: EntityInfoData;
  private deviceInfoFilter: DeviceInfoFilter;
  private resizeWindows: Subscription;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(DEVICE_FILTER_CONFIG_DATA)
              private data: DeviceFilterConfigData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: FormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef,
              private deviceProfileService: DeviceProfileService,
              private cd: ChangeDetectorRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.deviceInfoFilter = this.data.deviceInfoFilter;
      this.initialDeviceFilterConfig = this.data.initialFilterConfig;
      if (this.panelMode && !this.initialDeviceFilterConfig) {
        this.initialDeviceFilterConfig = deepClone(this.deviceInfoFilter);
      }
    }
    this.deviceInfoFilterForm = this.fb.group({
      deviceProfileId: [null, []],
      active: ['', []]
    });
    this.deviceInfoFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        if (!this.buttonMode) {
          this.deviceFilterUpdated(this.deviceInfoFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateDeviceInfoFilterForm(this.deviceInfoFilter);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceInfoFilterForm.disable({emitEvent: false});
    } else {
      this.deviceInfoFilterForm.enable({emitEvent: false});
    }
  }

  writeValue(deviceInfoFilter?: DeviceInfoFilter): void {
    this.deviceInfoFilter = deviceInfoFilter;
    if (!this.initialDeviceFilterConfig && deviceInfoFilter) {
      this.initialDeviceFilterConfig = deepClone(deviceInfoFilter);
    }
    this.updateButtonDisplayValue();
    this.updateDeviceInfoFilterForm(deviceInfoFilter);
  }

  toggleDeviceFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(this.nativeElement)
      .withPositions([connectedPosition]);

    this.deviceFilterOverlayRef = this.overlay.create(config);
    this.deviceFilterOverlayRef.backdropClick().subscribe(() => {
      this.deviceFilterOverlayRef.dispose();
    });
    this.deviceFilterOverlayRef.attach(new TemplatePortal(this.deviceFilterPanel,
      this.viewContainerRef));
    this.resizeWindows = fromEvent(window, 'resize').subscribe(() => {
      this.deviceFilterOverlayRef.updatePosition();
    });
  }

  cancel() {
    this.updateDeviceInfoFilterForm(this.deviceInfoFilter);
    this.deviceInfoFilterForm.markAsPristine();
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.deviceFilterOverlayRef.dispose();
    }
  }

  update() {
    this.deviceFilterUpdated(this.deviceInfoFilterForm.value);
    this.deviceInfoFilterForm.markAsPristine();
    if (this.panelMode) {
      this.panelResult = this.deviceInfoFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.resizeWindows.unsubscribe();
      this.deviceFilterOverlayRef.dispose();
    }
  }

  reset() {
    const deviceInfoFilter = this.deviceFilterFromFormValue(this.deviceInfoFilterForm.value);
    if (!this.deviceInfoFilterConfigEquals(deviceInfoFilter, this.initialDeviceFilterConfig)) {
      this.updateDeviceInfoFilterForm(this.initialDeviceFilterConfig);
      this.deviceInfoFilterForm.markAsDirty();
    }
  }

  deviceProfileChanged(deviceProfileInfo: EntityInfoData) {
    this.deviceProfileInfo = deviceProfileInfo;
    this.updateButtonDisplayValue();
  }

  private deviceInfoFilterConfigEquals = (filter1?: DeviceInfoFilter, filter2?: DeviceInfoFilter): boolean => {
    if (filter1 === filter2) {
      return true;
    }
    if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
      return true;
    } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
      if (filter1.active !== filter2.active) {
        return false;
      }
      return filter1.deviceProfileId === filter2.deviceProfileId;
    }
    return false;
  };

  private updateDeviceInfoFilterForm(deviceInfoFilter?: DeviceInfoFilter) {
    this.deviceInfoFilterForm.patchValue({
      deviceProfileId: deviceInfoFilter?.deviceProfileId ?? null,
      active: deviceInfoFilter?.active ?? null
    }, {emitEvent: false});
  }

  private deviceFilterUpdated(deviceInfoFilter: DeviceInfoFilter) {
    this.deviceInfoFilter = this.deviceFilterFromFormValue(deviceInfoFilter);
    this.updateButtonDisplayValue();
    this.propagateChange(this.deviceInfoFilter);
  }

  private updateButtonDisplayValue() {
    if (this.buttonMode) {
      const filterTextParts: string[] = [];
      if (isDefinedAndNotNull(this.deviceInfoFilter?.deviceProfileId)) {
        if (!this.deviceProfileInfo) {
          this.deviceProfileService.getDeviceProfileInfo(this.deviceInfoFilter?.deviceProfileId.id,
            {ignoreLoading: true, ignoreErrors: true}).subscribe(
            (deviceProfileInfo) => {
              this.deviceProfileChanged(deviceProfileInfo);
          });
          return;
        } else {
          filterTextParts.push(this.deviceProfileInfo.name);
        }
      }
      if (isDefinedAndNotNull(this.deviceInfoFilter?.active)) {
        const translationKey = this.deviceInfoFilter?.active ? 'device.active' : 'device.inactive';
        filterTextParts.push(this.translate.instant(translationKey));
      }
      if (!filterTextParts.length) {
        this.buttonDisplayValue = this.translate.instant('device.device-filter-title');
      } else {
        this.buttonDisplayValue = this.translate.instant('device.filter-title') + `: ${filterTextParts.join(', ')}`;
      }
      this.cd.detectChanges();
    }
  }

  private deviceFilterFromFormValue(formValue: any): DeviceInfoFilter {
    return {
      deviceProfileId: formValue?.deviceProfileId ?? null,
      active: formValue?.active ?? null
    };
  }
}
