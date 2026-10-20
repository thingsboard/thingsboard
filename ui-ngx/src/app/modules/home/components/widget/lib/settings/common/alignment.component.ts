// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  alignment, alignmentIcons, alignmentTranslations
} from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  AlignmentPanelComponent
} from '@home/components/widget/lib/settings/common/alignment-panel.component';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-alignment',
    templateUrl: './alignment.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AlignmentComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AlignmentComponent implements ControlValueAccessor {

  alignmentTranslations = alignmentTranslations;
  alignmentIcons = alignmentIcons;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  horizontal = true;

  @Input()
  allowedAlignments: alignment[];

  modelValue: alignment;

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: alignment): void {
    this.modelValue = value;
    if (!this.modelValue) {
      this.modelValue = this.horizontal ? 'left' : 'top';
    }
  }

  openAlignmentPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        alignment: this.modelValue,
        horizontal: this.horizontal,
        allowedAlignments: this.allowedAlignments
      };
      const alignmentPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: AlignmentPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['top', 'topLeft', 'topRight'],
        context: ctx,
        showCloseButton: false,
        isModal: false,
        popoverContentStyle: {padding: '6px'}
      });
      alignmentPanelPopover.tbComponentRef.instance.alignmentSelected.subscribe((alignment) => {
        alignmentPanelPopover.hide();
        this.modelValue = alignment;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
