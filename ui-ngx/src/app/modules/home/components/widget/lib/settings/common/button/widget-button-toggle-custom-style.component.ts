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
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatIconButton } from '@angular/material/button';
import { deepClone } from '@core/utils';
import {
  ButtonToggleAppearance,
  WidgetButtonToggleCustomStyle,
  WidgetButtonToggleState
} from '@home/components/widget/lib/button/segmented-button-widget.models';
import {
  WidgetButtonToggleCustomStylePanelComponent
} from '@home/components/widget/lib/settings/common/button/widget-button-toggle-custom-style-panel.component';

@Component({
  selector: 'tb-widget-button-toggle-custom-style',
  templateUrl: './widget-button-toggle-custom-style.component.html',
  styleUrls: ['./widget-button-toggle-custom-style.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetButtonToggleCustomStyleComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class WidgetButtonToggleCustomStyleComponent implements OnInit, OnChanges, ControlValueAccessor {

  @Input()
  disabled = false;

  @Input()
  value = false;

  @Input()
  appearance: ButtonToggleAppearance;

  @Input()
  borderRadius: string;

  @Input()
  autoScale: boolean;

  @Input()
  state: WidgetButtonToggleState;

  widgetButtonToggleState = WidgetButtonToggleState;

  modelValue: WidgetButtonToggleCustomStyle;

  previewAppearance: ButtonToggleAppearance;

  private propagateChange = (_val: any) => {};

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.updatePreviewAppearance();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange) {
        if (propName === 'appearance') {
          this.updatePreviewAppearance();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(_isDisabled: boolean): void {
  }

  writeValue(value: WidgetButtonToggleCustomStyle): void {
    this.modelValue = value;
    this.updatePreviewAppearance();
  }

  clearStyle() {
    this.updateModel(null);
  }

  openButtonCustomStylePopup($event: Event, matButton: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const widgetButtonCustomStylePanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: WidgetButtonToggleCustomStylePanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftTopOnly', 'leftOnly', 'leftBottomOnly'],
        context: {
          appearance: this.appearance,
          borderRadius: this.borderRadius,
          autoScale: this.autoScale,
          state: this.state,
          value: this.value,
          customStyle: this.modelValue
        },
        isModal: true
      });
      widgetButtonCustomStylePanelPopover.tbComponentRef.instance.popover = widgetButtonCustomStylePanelPopover;
      widgetButtonCustomStylePanelPopover.tbComponentRef.instance.customStyleApplied.subscribe((customStyle) => {
        widgetButtonCustomStylePanelPopover.hide();
        this.updateModel(customStyle);
      });
    }
  }

  private updateModel(value: WidgetButtonToggleCustomStyle): void {
    this.modelValue = value;
    this.updatePreviewAppearance();
    this.propagateChange(this.modelValue);
  }

  private updatePreviewAppearance() {
    this.previewAppearance = deepClone(this.appearance);
    if (this.modelValue) {
      if (this.value) {
        this.previewAppearance.selectedStyle.customStyle[this.state] = this.modelValue;
      } else {
        this.previewAppearance.unselectedStyle.customStyle[this.state] = this.modelValue;
      }
    }
    this.cd.markForCheck();
  }
}
