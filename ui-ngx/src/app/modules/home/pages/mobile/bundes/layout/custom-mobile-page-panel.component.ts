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
