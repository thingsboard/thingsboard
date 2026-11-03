// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { CustomMobilePage } from '@shared/models/mobile-app.models';
import { TbPopoverComponent } from '@shared/components/popover.component';

@Component({
    selector: 'tb-custom-menu-item-panel',
    templateUrl: './custom-mobile-page-panel.component.html',
    styleUrls: ['./custom-mobile-page-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CustomMobilePagePanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  pageItem: CustomMobilePage;

  @Input()
  popover: TbPopoverComponent<CustomMobilePagePanelComponent>;

  @Output()
  customMobilePageApplied = new EventEmitter<CustomMobilePage>();

  mobilePageControl = this.fb.control<CustomMobilePage>(null);

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.mobilePageControl.setValue(this.pageItem, {emitEvent: false});
    if (this.disabled) {
      this.mobilePageControl.disable({emitEvent: false});
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.mobilePageControl.valid) {
      const menuItem = this.mobilePageControl.value;
      this.customMobilePageApplied.emit(menuItem);
    }
  }
}
