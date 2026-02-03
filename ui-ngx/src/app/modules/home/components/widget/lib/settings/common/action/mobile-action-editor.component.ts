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

import { Component, DestroyRef, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  WidgetActionType,
  WidgetMobileActionDescriptor,
  WidgetMobileActionType,
  widgetMobileActionTypeTranslationMap,
} from '@shared/models/widget.models';
import { CustomActionEditorCompleter } from '@home/components/widget/lib/settings/common/action/custom-action.models';
import {
  getDefaultGetLocationFunction,
  getDefaultGetPhoneNumberFunction,
  getDefaultHandleEmptyResultFunction,
  getDefaultHandleErrorFunction,
  getDefaultProcessImageFunction,
  getDefaultProcessLaunchResultFunction,
  getDefaultProcessLocationFunction,
  getDefaultProcessQrCodeFunction,
  getDefaultProvisionSuccessFunction
} from '@home/components/widget/lib/settings/common/action/mobile-action-editor.models';
import { WidgetService } from '@core/http/widget.service';
import { TbFunction } from '@shared/models/js-function.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-mobile-action-editor',
    templateUrl: './mobile-action-editor.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MobileActionEditorComponent),
            multi: true
        }],
    standalone: false
})
export class MobileActionEditorComponent implements ControlValueAccessor, OnInit {

  mobileActionTypes = Object.keys(WidgetMobileActionType);
  mobileActionTypeTranslations = widgetMobileActionTypeTranslationMap;
  mobileActionType = WidgetMobileActionType;

  customActionEditorCompleter = CustomActionEditorCompleter;

  mobileActionFormGroup: UntypedFormGroup;
  mobileActionTypeFormGroup: UntypedFormGroup;

  functionScopeVariables: string[];

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (_v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              private destroyRef: DestroyRef) {
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.mobileActionFormGroup = this.fb.group({
      type: [null, Validators.required],
      handleEmptyResultFunction: [null],
      handleErrorFunction: [null]
    });
    this.mobileActionFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type: WidgetMobileActionType) => {
      let action: WidgetMobileActionDescriptor = this.mobileActionFormGroup.value;
      if (this.mobileActionTypeFormGroup) {
        action = {...action, ...this.mobileActionTypeFormGroup.value};
      }
      this.updateMobileActionType(type, action);
    });
    this.mobileActionFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.mobileActionFormGroup.disable({emitEvent: false});
      if (this.mobileActionTypeFormGroup) {
        this.mobileActionTypeFormGroup.disable({emitEvent: false});
      }
    } else {
      this.mobileActionFormGroup.enable({emitEvent: false});
      if (this.mobileActionTypeFormGroup) {
        this.mobileActionTypeFormGroup.enable({emitEvent: false});
      }
    }
  }

  writeValue(value: WidgetMobileActionDescriptor | null): void {
    this.mobileActionFormGroup.patchValue({type: value?.type,
                                                 handleEmptyResultFunction: value?.handleEmptyResultFunction,
                                                 handleErrorFunction: value?.handleErrorFunction}, {emitEvent: false});
    this.updateMobileActionType(value?.type, value);
  }

  private updateModel() {
    let descriptor: WidgetMobileActionDescriptor = null;
    if (this.mobileActionFormGroup.valid && this.mobileActionTypeFormGroup.valid) {
      descriptor = { ...this.mobileActionFormGroup.getRawValue(), ...this.mobileActionTypeFormGroup.getRawValue() };
    }
    this.propagateChange(descriptor);
  }

  private updateMobileActionType(type?: WidgetMobileActionType, action?: WidgetMobileActionDescriptor) {
    const prevType = action?.type;
    const targetType = type || prevType;
    const changed = prevType !== type;
    if (changed && targetType) {
      let handleEmptyResultFunction = action?.handleEmptyResultFunction;
      const defaultHandleEmptyResultFunction = getDefaultHandleEmptyResultFunction(targetType);
      if (defaultHandleEmptyResultFunction !== handleEmptyResultFunction) {
        handleEmptyResultFunction = getDefaultHandleEmptyResultFunction(type);
        this.mobileActionFormGroup.patchValue({handleEmptyResultFunction}, {emitEvent: false});
      }
      let handleErrorFunction = action?.handleErrorFunction;
      const defaultHandleErrorFunction = getDefaultHandleErrorFunction(targetType);
      if (defaultHandleErrorFunction !== handleErrorFunction) {
        handleErrorFunction = getDefaultHandleErrorFunction(type);
        this.mobileActionFormGroup.patchValue({handleErrorFunction}, {emitEvent: false});
      }
    }
    this.mobileActionTypeFormGroup = this.fb.group({});
    if (type) {
      let processLaunchResultFunction: TbFunction;
      switch (type) {
        case WidgetMobileActionType.takePictureFromGallery:
        case WidgetMobileActionType.takePhoto:
        case WidgetMobileActionType.takeScreenshot:
          let processImageFunction = action?.processImageFunction;
          if (changed) {
            const defaultProcessImageFunction = getDefaultProcessImageFunction(targetType);
            if (defaultProcessImageFunction !== processImageFunction) {
              processImageFunction = getDefaultProcessImageFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processImageFunction',
            this.fb.control(processImageFunction, [])
          );
          break;
        case WidgetMobileActionType.mapDirection:
        case WidgetMobileActionType.mapLocation:
          let getLocationFunction = action?.getLocationFunction;
          processLaunchResultFunction = action?.processLaunchResultFunction;
          if (changed) {
            const defaultGetLocationFunction = getDefaultGetLocationFunction();
            if (defaultGetLocationFunction !== getLocationFunction) {
              getLocationFunction = defaultGetLocationFunction;
            }
            const defaultProcessLaunchResultFunction = getDefaultProcessLaunchResultFunction(targetType);
            if (defaultProcessLaunchResultFunction !== processLaunchResultFunction) {
              processLaunchResultFunction = getDefaultProcessLaunchResultFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'getLocationFunction',
            this.fb.control(getLocationFunction, [Validators.required])
          );
          this.mobileActionTypeFormGroup.addControl(
            'processLaunchResultFunction',
            this.fb.control(processLaunchResultFunction, [])
          );
          break;
        case WidgetMobileActionType.scanQrCode:
          let processQrCodeFunction = action?.processQrCodeFunction;
          if (changed) {
            const defaultProcessQrCodeFunction = getDefaultProcessQrCodeFunction();
            if (defaultProcessQrCodeFunction !== processQrCodeFunction) {
              processQrCodeFunction = defaultProcessQrCodeFunction;
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processQrCodeFunction',
            this.fb.control(processQrCodeFunction, [])
          );
          break;
        case WidgetMobileActionType.makePhoneCall:
          let getPhoneNumberFunction = action?.getPhoneNumberFunction;
          processLaunchResultFunction = action?.processLaunchResultFunction;
          if (changed) {
            const defaultGetPhoneNumberFunction = getDefaultGetPhoneNumberFunction();
            if (defaultGetPhoneNumberFunction !== getPhoneNumberFunction) {
              getPhoneNumberFunction = defaultGetPhoneNumberFunction;
            }
            const defaultProcessLaunchResultFunction = getDefaultProcessLaunchResultFunction(targetType);
            if (defaultProcessLaunchResultFunction !== processLaunchResultFunction) {
              processLaunchResultFunction = getDefaultProcessLaunchResultFunction(type);
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'getPhoneNumberFunction',
            this.fb.control(getPhoneNumberFunction, [Validators.required])
          );
          this.mobileActionTypeFormGroup.addControl(
            'processLaunchResultFunction',
            this.fb.control(processLaunchResultFunction, [])
          );
          break;
        case WidgetMobileActionType.getLocation:
          let processLocationFunction = action?.processLocationFunction;
          if (changed) {
            const defaultProcessLocationFunction = getDefaultProcessLocationFunction();
            if (defaultProcessLocationFunction !== processLocationFunction) {
              processLocationFunction = defaultProcessLocationFunction;
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'processLocationFunction',
            this.fb.control(processLocationFunction, [Validators.required])
          );
          break;
        case WidgetMobileActionType.deviceProvision:
          let handleProvisionSuccessFunction = action?.handleProvisionSuccessFunction;
          if (changed) {
            const defaultProvisionSuccessFunction = getDefaultProvisionSuccessFunction();
            if (defaultProvisionSuccessFunction !== handleProvisionSuccessFunction) {
              handleProvisionSuccessFunction = defaultProvisionSuccessFunction;
            }
          }
          this.mobileActionTypeFormGroup.addControl(
            'handleProvisionSuccessFunction',
            this.fb.control(handleProvisionSuccessFunction, [Validators.required])
          );
      }
    }
    this.mobileActionTypeFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  protected readonly WidgetActionType = WidgetActionType;
}
