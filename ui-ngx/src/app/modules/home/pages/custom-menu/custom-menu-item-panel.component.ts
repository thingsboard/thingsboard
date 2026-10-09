// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { CMScope, CustomMenuItem } from '@shared/models/custom-menu.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormControl } from '@angular/forms';

@Component({
    selector: 'tb-custom-menu-item-panel',
    templateUrl: './custom-menu-item-panel.component.html',
    styleUrls: ['./custom-menu-item-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CustomMenuItemPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  scope: CMScope;

  @Input()
  subItem: boolean;

  @Input()
  menuItem: CustomMenuItem;

  @Input()
  popover: TbPopoverComponent<CustomMenuItemPanelComponent>;

  @Output()
  customMenuItemApplied = new EventEmitter<CustomMenuItem>();

  title: string;

  customMenuItemControl: UntypedFormControl;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.customMenuItemControl = this.fb.control(this.menuItem);
    this.title = this.subItem ? 'custom-menu.edit-custom-menu-subitem' : 'custom-menu.edit-custom-menu-item';
    if (this.disabled) {
      this.customMenuItemControl.disable({emitEvent: false});
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.customMenuItemControl.valid) {
      const menuItem: CustomMenuItem = this.customMenuItemControl.value;
      this.customMenuItemApplied.emit(menuItem);
    }
  }
}
