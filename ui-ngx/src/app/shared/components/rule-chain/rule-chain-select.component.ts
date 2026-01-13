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

import { Component, forwardRef, Injector, Input, StaticProvider, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TooltipPosition } from '@angular/material/tooltip';
import { RuleChain, RuleChainType } from '@shared/models/rule-chain.models';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import {
  RULE_CHAIN_SELECT_PANEL_DATA, RuleChainSelectPanelComponent,
  RuleChainSelectPanelData
} from '@shared/components/rule-chain/rule-chain-select-panel.component';
import { ComponentPortal } from '@angular/cdk/portal';
import { POSITION_MAP } from '@shared/models/overlay.models';

@Component({
  selector: 'tb-rule-chain-select',
  templateUrl: './rule-chain-select.component.html',
  styleUrls: ['./rule-chain-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleChainSelectComponent),
    multi: true
  }]
})
export class RuleChainSelectComponent implements ControlValueAccessor {

  @Input()
  tooltipPosition: TooltipPosition = 'above';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  ruleChainType: RuleChainType = RuleChainType.CORE;

  ruleChain: RuleChain | null;

  panelOpened = false;

  private propagateChange = (v: any) => { };

  constructor(private overlay: Overlay,
              private viewContainerRef: ViewContainerRef) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: RuleChain): void {
    if (isDefinedAndNotNull(value)) {
      this.ruleChain = value;
    }
  }

  ruleChainIdChanged() {
    this.updateView();
  }

  openRuleChainSelectPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.disabled) {
      const target = $event.currentTarget;
      const config = new OverlayConfig({
        panelClass: 'tb-filter-panel',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true,
        width: (target as HTMLElement).offsetWidth
      });
      config.positionStrategy = this.overlay.position()
        .flexibleConnectedTo(target as HTMLElement)
        .withPositions([POSITION_MAP.bottom]);
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });
      const providers: StaticProvider[] = [
        {
          provide: RULE_CHAIN_SELECT_PANEL_DATA,
          useValue: {
            ruleChainId: this.ruleChain.id?.id,
            ruleChainType: this.ruleChainType
          } as RuleChainSelectPanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      const componentRef = overlayRef.attach(new ComponentPortal(RuleChainSelectPanelComponent,
        this.viewContainerRef, injector));
      this.panelOpened = true;
      componentRef.onDestroy(() => {
        this.panelOpened = false;
        if (componentRef.instance.ruleChainSelected) {
          this.ruleChain = componentRef.instance.result;
          this.updateView();
        }
      });
    }
  }

  private updateView() {
    this.propagateChange(this.ruleChain);
  }

}
