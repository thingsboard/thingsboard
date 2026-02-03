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
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { SetValueAction, SetValueSettings, ValueToDataType } from '@shared/models/action-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  SetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/set-value-action-settings-panel.component';
import { ValueType } from '@shared/models/constants';

@Component({
    selector: 'tb-set-value-action-settings',
    templateUrl: './action-settings-button.component.html',
    styleUrls: ['./action-settings-button.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SetValueActionSettingsComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class SetValueActionSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  panelTitle: string;

  @Input()
  valueType = ValueType.BOOLEAN;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  disabled = false;

  modelValue: SetValueSettings;

  displayValue: string;

  private propagateChange = null;

  constructor(private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabled !== isDisabled) {
      this.disabled = isDisabled;
    }
  }

  writeValue(value: SetValueSettings): void {
    this.modelValue = value;
    this.updateDisplayValue();
  }

  openActionSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
     const setValueSettingsPanelPopover = this.popoverService.displayPopover({
       trigger,
       renderer: this.renderer,
       componentType: SetValueActionSettingsPanelComponent,
       hostView: this.viewContainerRef,
       preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
       context: {
         setValueSettings: this.modelValue,
         panelTitle: this.panelTitle,
         valueType: this.valueType,
         aliasController: this.aliasController,
         targetDevice: this.targetDevice,
         widgetType: this.widgetType
       },
       isModal: true
     });
      setValueSettingsPanelPopover.tbComponentRef.instance.popover = setValueSettingsPanelPopover;
      setValueSettingsPanelPopover.tbComponentRef.instance.setValueSettingsApplied.subscribe((setValueSettings) => {
        setValueSettingsPanelPopover.hide();
        this.modelValue = setValueSettings;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    let value: any;
    switch (this.modelValue.valueToData.type) {
      case ValueToDataType.VALUE:
        value = 'value';
        break;
      case ValueToDataType.CONSTANT:
        value = this.modelValue.valueToData.constantValue;
        break;
      case ValueToDataType.FUNCTION:
        value = 'f(value)';
        break;
      case ValueToDataType.NONE:
        break;
    }
    switch (this.modelValue.action) {
      case SetValueAction.EXECUTE_RPC:
        let methodName = this.modelValue.executeRpc.method;
        if (isDefinedAndNotNull(value)) {
          methodName = `${methodName}(${value})`;
        }
        this.displayValue = this.translate.instant('widgets.value-action.execute-rpc-text', {methodName});
        break;
      case SetValueAction.SET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.value-action.set-attribute-to-value-text',
          {key: this.modelValue.setAttribute.key, value});
        break;
      case SetValueAction.ADD_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.value-action.add-time-series-value-text',
          {key: this.modelValue.putTimeSeries.key, value});
        break;
    }
    this.cd.markForCheck();
  }

}
