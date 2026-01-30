///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  OnDestroy,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { DeviceInfoFilter } from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { EntityInfoData } from '@shared/models/entity.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export const DEVICE_FILTER_CONFIG_DATA = new InjectionToken<any>('DeviceFilterConfigData');

export interface DeviceFilterConfigData {
  panelMode: boolean;
  deviceInfoFilter: DeviceInfoFilter;
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
    ],
    standalone: false
})
export class DeviceInfoFilterComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @ViewChild('deviceFilterPanel')
  deviceFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  panelMode = false;

  buttonDisplayValue = this.translate.instant('device.device-filter');

  deviceInfoFilterForm: UntypedFormGroup;

  deviceFilterOverlayRef: OverlayRef;

  panelResult: DeviceInfoFilter = null;

  private deviceProfileInfo: EntityInfoData;

  private deviceInfoFilter: DeviceInfoFilter;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(DEVICE_FILTER_CONFIG_DATA)
              private data: DeviceFilterConfigData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
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
    }
    this.deviceInfoFilterForm = this.fb.group({
      deviceProfileId: [null, []],
      active: ['', []]
    });
    this.deviceInfoFilterForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.updateValidators();
        if (!this.buttonMode) {
          this.deviceFilterUpdated(this.deviceInfoFilterForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateDeviceInfoFilterForm(this.deviceInfoFilter);
    }
  }

  ngOnDestroy(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.deviceInfoFilterForm.disable({emitEvent: false});
    } else {
      this.deviceInfoFilterForm.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(deviceInfoFilter?: DeviceInfoFilter): void {
    this.deviceInfoFilter = deviceInfoFilter;
    this.updateButtonDisplayValue();
    this.updateDeviceInfoFilterForm(deviceInfoFilter);
  }

  private updateValidators() {
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
  }

  cancel() {
    this.updateDeviceInfoFilterForm(this.deviceInfoFilter);
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.deviceFilterOverlayRef.dispose();
    }
  }

  update() {
    this.deviceFilterUpdated(this.deviceInfoFilterForm.value);
    if (this.panelMode) {
      this.panelResult = this.deviceInfoFilter;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.deviceFilterOverlayRef.dispose();
    }
  }

  deviceProfileChanged(deviceProfileInfo: EntityInfoData) {
    this.deviceProfileInfo = deviceProfileInfo;
    this.updateButtonDisplayValue();
  }

  private updateDeviceInfoFilterForm(deviceInfoFilter?: DeviceInfoFilter) {
    this.deviceInfoFilterForm.patchValue({
      deviceProfileId: deviceInfoFilter?.deviceProfileId,
      active: isDefinedAndNotNull(deviceInfoFilter?.active) ? deviceInfoFilter?.active : ''
    }, {emitEvent: false});
    this.updateValidators();
  }

  private deviceFilterUpdated(deviceInfoFilter: DeviceInfoFilter) {
    this.deviceInfoFilter = deviceInfoFilter;
    if ((this.deviceInfoFilter.active as any) === '') {
      this.deviceInfoFilter.active = null;
    }
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

}
