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

import { Component, DestroyRef, EventEmitter, inject, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { DefaultMobilePage, defaultMobilePageMap, hideDefaultMenuItems } from '@shared/models/mobile-app.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-default-mobile-page-panel',
  templateUrl: './default-mobile-page-panel.component.html',
  styleUrls: ['./default-mobile-page-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DefaultMobilePagePanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  pageItem: DefaultMobilePage;

  @Input()
  popover: TbPopoverComponent<DefaultMobilePagePanelComponent>;

  @Output()
  defaultMobilePageApplied = new EventEmitter<DefaultMobilePage>();

  mobilePageFormGroup = this.fb.group({
    visible: [true],
    icon: [''],
    label: ['', [Validators.pattern(/\S/), Validators.maxLength(255)]],
  });

  isCleanupEnabled = false;
  defaultItemName: string;

  private defaultMobilePages: Omit<DefaultMobilePage, 'type' | 'visible'>;
  private destroyRef = inject(DestroyRef);

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.defaultMobilePages = defaultMobilePageMap.get(this.pageItem.id);
    this.defaultItemName = this.defaultMobilePages.label;

    this.mobilePageFormGroup.patchValue({
      label: this.pageItem.label,
      icon: this.pageItem.icon ? this.pageItem.icon : this.defaultMobilePages.icon,
      visible: this.pageItem.visible
    }, {emitEvent: false});

    if (this.disabled) {
      this.mobilePageFormGroup.disable({emitEvent: false});
    } else {
      this.mobilePageFormGroup.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateModel();
      });
      this.updateCleanupState();
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    this.defaultMobilePageApplied.emit(this.pageItem);
  }

  cleanup() {
    this.mobilePageFormGroup.patchValue({
      visible: !hideDefaultMenuItems.includes(this.pageItem.id),
      icon: this.defaultMobilePages.icon,
      label: null
    }, {emitEvent: false});
    this.mobilePageFormGroup.markAsDirty();
    this.updateModel();
  }

  private updateCleanupState() {
    this.isCleanupEnabled = (hideDefaultMenuItems.includes(this.pageItem.id) ? this.pageItem.visible : !this.pageItem.visible) ||
      !!this.pageItem.label ||
      !!this.pageItem.icon;
  }

  private updateModel() {
    this.pageItem.visible = this.mobilePageFormGroup.get('visible').value;
    const label = this.mobilePageFormGroup.get('label').value;
    if (label) {
      this.pageItem.label = label;
    } else {
      delete this.pageItem.label;
    }
    const icon = this.mobilePageFormGroup.get('icon').value;
    if (icon !== this.defaultMobilePages.icon) {
      this.pageItem.icon = icon;
    } else {
      delete this.pageItem.icon;
    }
    this.updateCleanupState();
  }
}
