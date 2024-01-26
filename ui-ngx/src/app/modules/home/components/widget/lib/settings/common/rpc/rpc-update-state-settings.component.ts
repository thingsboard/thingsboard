///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import {
  RpcStateToParamsType,
  RpcUpdateStateAction,
  RpcUpdateStateSettings
} from '@shared/models/rpc-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { ValueType } from '@shared/models/constants';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice } from '@shared/models/widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import {
  RpcUpdateStateSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-update-state-settings-panel.component';

@Component({
  selector: 'tb-rpc-update-state-settings',
  templateUrl: './rpc-state-settings-button.component.html',
  styleUrls: ['./rpc-state-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RpcUpdateStateSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class RpcUpdateStateSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  panelTitle: string;

  @Input()
  stateValueType: ValueType;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  disabled = false;

  modelValue: RpcUpdateStateSettings;

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

  writeValue(value: RpcUpdateStateSettings): void {
    this.modelValue = value;
    this.updateDisplayValue();
  }

  openRpcStateSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        updateState: this.modelValue,
        panelTitle: this.panelTitle,
        stateValueType: this.stateValueType,
        aliasController: this.aliasController,
        targetDevice: this.targetDevice
      };
     const updateStateSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, RpcUpdateStateSettingsPanelComponent,
       ['leftTopOnly', 'leftOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      updateStateSettingsPanelPopover.tbComponentRef.instance.popover = updateStateSettingsPanelPopover;
      updateStateSettingsPanelPopover.tbComponentRef.instance.updateStateSettingsApplied.subscribe((updateState) => {
        updateStateSettingsPanelPopover.hide();
        this.modelValue = updateState;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    let value: any;
    switch (this.modelValue.stateToParams.type) {
      case RpcStateToParamsType.CONSTANT:
        value = this.modelValue.stateToParams.constantValue;
        break;
      case RpcStateToParamsType.FUNCTION:
        value = 'f(value)';
        break;
      case RpcStateToParamsType.NONE:
        break;
    }
    switch (this.modelValue.action) {
      case RpcUpdateStateAction.EXECUTE_RPC:
        let methodName = this.modelValue.executeRpc.method;
        if (isDefinedAndNotNull(value)) {
          methodName = `${methodName}(${value})`;
        }
        this.displayValue = this.translate.instant('widgets.rpc-state.execute-rpc-text', {methodName});
        break;
      case RpcUpdateStateAction.SET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.rpc-state.set-attribute-to-value-text',
          {key: this.modelValue.setAttribute.key, value});
        break;
      case RpcUpdateStateAction.ADD_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.rpc-state.add-time-series-value-text',
          {key: this.modelValue.setAttribute.key, value});
        break;
    }
    this.cd.markForCheck();
  }

}
