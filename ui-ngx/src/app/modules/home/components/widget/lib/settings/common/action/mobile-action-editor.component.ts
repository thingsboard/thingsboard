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
  ActionConfig,
  ProvisionType,
  provisionTypeTranslationMap,
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
  getDefaultHandleNonMobileFallBackFunction,
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
  }]
})
export class MobileActionEditorComponent implements ControlValueAccessor, OnInit {

  mobileActionTypes = Object.keys(WidgetMobileActionType);
  mobileActionTypeTranslations = widgetMobileActionTypeTranslationMap;
  mobileActionType = WidgetMobileActionType;

  customActionEditorCompleter = CustomActionEditorCompleter;

  mobileActionFormGroup: UntypedFormGroup;
  mobileActionTypeFormGroup: UntypedFormGroup;

  functionScopeVariables: string[];

  actionConfig: ActionConfig[];
  commonActionConfig: ActionConfig[];

  provisionTypes: string[] = Object.keys(ProvisionType);
  provisionTypeTranslationMap = provisionTypeTranslationMap;

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
      handleErrorFunction: [null],
      handleNonMobileFallbackFunction: [null]
    });
    this.getCommonActionConfigs();
    this.mobileActionFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((type: WidgetMobileActionType) => {
      let action: WidgetMobileActionDescriptor = this.mobileActionFormGroup.value;
      if (this.mobileActionTypeFormGroup) {
        action = {...action, ...this.mobileActionTypeFormGroup.value};
      }
      this.updateMobileActionType(type, action);
      this.getActionConfigs();
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
    this.mobileActionFormGroup.patchValue({
      type: value?.type,
      handleEmptyResultFunction: value?.handleEmptyResultFunction,
      handleErrorFunction: value?.handleErrorFunction,
      handleNonMobileFallbackFunction: value?.handleNonMobileFallbackFunction
    }, {emitEvent: false});
    this.updateMobileActionType(value?.type, value);
    this.getActionConfigs();
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
      let handleNonMobileFallbackFunction = action?.handleNonMobileFallbackFunction;
      const defaultHandleNonMobileFallbackFunction = getDefaultHandleNonMobileFallBackFunction();
      if (defaultHandleNonMobileFallbackFunction !== handleNonMobileFallbackFunction) {
        handleNonMobileFallbackFunction = getDefaultHandleNonMobileFallBackFunction();
        this.mobileActionFormGroup.patchValue({handleNonMobileFallbackFunction}, {emitEvent: false});
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
          this.mobileActionTypeFormGroup.addControl(
            'saveToGallery',
            this.fb.control(action?.saveToGallery || false, [])
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
          this.mobileActionTypeFormGroup.addControl(
            'provisionType',
            this.fb.control(action?.provisionType || ProvisionType.auto, [])
          );
      }
    }
    this.mobileActionTypeFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  getActionConfigs() {
    const type = this.mobileActionFormGroup.get('type').value;
    this.actionConfig = [];
    switch (type) {
      case this.mobileActionType.deviceProvision:
        this.actionConfig.push({
          title: 'widget-action.mobile.handle-provision-success-function',
          formControlName: 'handleProvisionSuccessFunction',
          functionName: 'handleProvisionSuccess',
          functionArgs: ['deviceName', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel']
        });
        break;
      case this.mobileActionType.mapDirection:
      case this.mobileActionType.mapLocation:
        this.actionConfig.push({
          title: 'widget-action.mobile.get-location-function',
          formControlName: 'getLocationFunction',
          functionName: 'getLocation',
          functionArgs: ['$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_get_location_fn'
        });
        this.actionConfig.push({
          title: 'widget-action.mobile.process-launch-result-function',
          formControlName: 'processLaunchResultFunction',
          functionName: 'processLaunchResult',
          functionArgs: ['launched', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_process_launch_result_fn'
        });
        break;
      case this.mobileActionType.makePhoneCall:
        this.actionConfig.push({
          title: 'widget-action.mobile.get-phone-number-function',
          formControlName: 'getPhoneNumberFunction',
          functionName: 'getPhoneNumber',
          functionArgs: ['$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_get_phone_number_fn'
        });
        this.actionConfig.push({
          title: 'widget-action.mobile.process-launch-result-function',
          formControlName: 'processLaunchResultFunction',
          functionName: 'processLaunchResult',
          functionArgs: ['launched', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_process_launch_result_fn'
        });
        break;
      case this.mobileActionType.takePhoto:
      case this.mobileActionType.takePictureFromGallery:
      case this.mobileActionType.takeScreenshot:
        this.actionConfig.push({
          title: 'widget-action.mobile.process-image-function',
          formControlName: 'processImageFunction',
          functionName: 'processImage',
          functionArgs: ['imageUrl', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_process_image_fn'
        });
        break;
      case this.mobileActionType.scanQrCode:
        this.actionConfig.push({
          title: 'widget-action.mobile.process-qr-code-function',
          formControlName: 'processQrCodeFunction',
          functionName: 'processQrCode',
          functionArgs: ['code', 'format', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_process_qr_code_fn'
        });
        break;
      case this.mobileActionType.getLocation:
        this.actionConfig.push({
          title: 'widget-action.mobile.process-location-function',
          formControlName: 'processLocationFunction',
          functionName: 'processLocation',
          functionArgs: ['latitude', 'longitude', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
          helpId: 'widget/action/mobile_process_location_fn'
        });
        break;
    }
  }

  getCommonActionConfigs() {
    this.commonActionConfig = [
      {
        title: 'widget-action.mobile.handle-empty-result-function',
        formControlName: 'handleEmptyResultFunction',
        functionName: 'handleEmptyResult',
        functionArgs: ['$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
        helpId: 'widget/action/mobile_handle_empty_result_fn'
      },
      {
        title: 'widget-action.mobile.handle-error-function',
        formControlName: 'handleErrorFunction',
        functionName: 'handleError',
        functionArgs: ['error', '$event', 'widgetContext', 'entityId', 'entityName', 'additionalParams', 'entityLabel'],
        helpId: 'widget/action/mobile_handle_error_fn'
      },
      {
        title: 'widget-action.mobile.handle-non-mobile-fallback-function',
        formControlName: 'handleNonMobileFallbackFunction',
        functionName: 'handleNonMobileFallback',
        functionArgs: ['$event', 'widgetContext'],
        helpId: 'widget/action/mobile_handle_non_mobile_fallback_fn'
      }
    ];
  }

  protected readonly WidgetActionType = WidgetActionType;
}
