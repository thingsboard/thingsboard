// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        }],
    standalone: false
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
