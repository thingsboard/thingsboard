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
import { RpcInitialStateAction, RpcInitialStateSettings } from '@shared/models/rpc-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { ValueType } from '@shared/models/constants';
import {
  RpcInitialStateSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/rpc/rpc-initial-state-settings-panel.component';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice } from '@shared/models/widget.models';

@Component({
  selector: 'tb-rpc-initial-state-settings',
  templateUrl: './rpc-state-settings-button.component.html',
  styleUrls: ['./rpc-state-settings-button.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RpcInitialStateSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class RpcInitialStateSettingsComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.overflow')
  overflow = 'hidden';

  @Input()
  stateValueType: ValueType;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  disabled = false;

  modelValue: RpcInitialStateSettings<any>;

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

  writeValue(value: RpcInitialStateSettings<any>): void {
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
        initialState: this.modelValue,
        stateValueType: this.stateValueType,
        aliasController: this.aliasController,
        targetDevice: this.targetDevice
      };
      const initialStateSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, RpcInitialStateSettingsPanelComponent,
        ['leftTopOnly', 'leftOnly', 'leftBottomOnly'], true, null,
        ctx,
        {},
        {}, {}, true);
      initialStateSettingsPanelPopover.tbComponentRef.instance.popover = initialStateSettingsPanelPopover;
      initialStateSettingsPanelPopover.tbComponentRef.instance.initialStateSettingsApplied.subscribe((initialState) => {
        initialStateSettingsPanelPopover.hide();
        this.modelValue = initialState;
        this.updateDisplayValue();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateDisplayValue() {
    switch (this.modelValue.action) {
      case RpcInitialStateAction.DO_NOTHING:
        if (this.stateValueType === ValueType.BOOLEAN) {
          this.displayValue = this.translate.instant(!!this.modelValue.defaultValue ? 'widgets.rpc-state.on' : 'widgets.rpc-state.off');
        } else {
          this.displayValue = this.modelValue.defaultValue + '';
        }
        break;
      case RpcInitialStateAction.EXECUTE_RPC:
        const methodName = this.modelValue.executeRpc.method;
        this.displayValue = this.translate.instant('widgets.rpc-state.execute-rpc-text', {methodName});
        break;
      case RpcInitialStateAction.GET_ATTRIBUTE:
        this.displayValue = this.translate.instant('widgets.rpc-state.get-attribute-text', {key: this.modelValue.getAttribute.key});
        break;
      case RpcInitialStateAction.GET_TIME_SERIES:
        this.displayValue = this.translate.instant('widgets.rpc-state.get-time-series-text', {key: this.modelValue.getTimeSeries.key});
        break;
    }
    this.cd.markForCheck();
  }

}
