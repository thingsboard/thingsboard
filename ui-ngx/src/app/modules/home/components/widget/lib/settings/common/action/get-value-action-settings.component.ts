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
import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { ValueType } from '@shared/models/constants';
import {
  GetValueActionSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/action/get-value-action-settings-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';

@Component({
  selector: 'tb-get-value-action-settings',
  templateUrl: './action-settings-button.component.html',
  styleUrls: ['./action-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GetValueActionSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class GetValueActionSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  panelTitle: string;

  @Input()
  valueType: ValueType;

  @Input()
  trueLabel: string;

  @Input()
  falseLabel: string;

  @Input()
  stateLabel: string;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  widgetType: widgetType;

  @Input()
  disabled = false;

  modelValue: GetValueSettings<any>;

  displayValue: string;

  private propagateChange = null;

  constructor(private translate: TranslateService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    if (!this.trueLabel) {
      this.trueLabel = this.translate.instant('value.true');
    }
    if (!this.falseLabel) {
      this.falseLabel = this.translate.instant('value.false');
    }
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

  writeValue(value: GetValueSettings<any>): void {
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
      const getValueSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: GetValueActionSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          getValueSettings: this.modelValue,
          panelTitle: this.panelTitle,
          valueType: this.valueType,
          trueLabel: this.trueLabel,
          falseLabel: this.falseLabel,
          stateLabel: this.stateLabel,
          aliasController: this.aliasController,
          targetDevice: this.targetDevice,
          widgetType: this.widgetType
        },
        isModal: true
      });
      getValueSettingsPanelPopover.tbComponentRef.instance.popover = getValueSettingsPanelPopover;
      getValueSettingsPanelPopover.tbComponentRef.instance.getValueSettingsApplied.subscribe((getValueSettings) => {
        getValueSettingsPanelPopover.hide();
        this.modelValue = getValueSettings;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    switch (this.modelValue.action) {
      case GetValueAction.DO_NOTHING:
        if (this.valueType === ValueType.BOOLEAN) {
          this.displayValue =
            !!this.modelValue.defaultValue ? this.trueLabel : this.falseLabel;
        } else {
          this.displayValue = this.modelValue.defaultValue + '';
        }
        break;
      case GetValueAction.EXECUTE_RPC:
        const methodName = this.modelValue.executeRpc.method;
        this.displayValue = this.translate.instant('widgets.value-action.execute-rpc-text', {methodName});
        break;
      case GetValueAction.GET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.value-action.get-attribute-text', {key: this.modelValue.getAttribute.key});
        break;
      case GetValueAction.GET_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.value-action.get-time-series-text', {key: this.modelValue.getTimeSeries.key});
        break;
      case GetValueAction.GET_ALARM_STATUS:
        this.displayValue = this.translate.instant('widgets.value-action.get-alarm-status-text');
        break;
      case GetValueAction.GET_DASHBOARD_STATE:
        if (this.valueType === ValueType.BOOLEAN) {
          const state = this.modelValue.dataToValue?.compareToValue;
          if (this.modelValue.dataToValue?.type === DataToValueType.FUNCTION) {
            this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-function-is-text', {state});
          } else {
            this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-is-text', {state});
          }
        } else {
          this.displayValue = this.translate.instant('widgets.value-action.get-dashboard-state-text');
        }
        break;
      case GetValueAction.GET_DASHBOARD_STATE_OBJECT:
        if (this.valueType === ValueType.BOOLEAN) {
          const state = this.modelValue.dataToValue?.compareToValue;
          this.displayValue = this.translate.instant('widgets.value-action.when-dashboard-state-object-function-is-text', {state});
        } else {
          this.displayValue = this.translate.instant('widgets.value-action.get-dashboard-state-object-text');
        }
        break;
    }
    this.cd.markForCheck();
  }

}
