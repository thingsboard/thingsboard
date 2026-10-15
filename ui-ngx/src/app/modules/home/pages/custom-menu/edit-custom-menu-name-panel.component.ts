// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { CustomMenuService } from '@core/http/custom-menu.service';

@Component({
    selector: 'tb-edit-custom-menu-name-panel',
    templateUrl: './edit-custom-menu-name-panel.component.html',
    styleUrls: ['./edit-custom-menu-name-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditCustomMenuNamePanelComponent implements OnInit {

  @Input()
  customMenuId: string;

  @Input()
  name: string;

  @Input()
  popover: TbPopoverComponent<EditCustomMenuNamePanelComponent>;

  @Output()
  nameApplied = new EventEmitter<string>();

  nameFormControl = this.fb.control(null, [Validators.required]);

  constructor(private fb: UntypedFormBuilder,
              private customMenuService: CustomMenuService) {}

  ngOnInit(): void {
    this.nameFormControl.setValue(this.name, {emitEvent: false});
  }

  cancel() {
    this.popover?.hide();
  }

  applyName() {
    const name = this.nameFormControl.value;
    this.customMenuService.updateCustomMenuName(this.customMenuId, name).subscribe(() => {
      this.nameApplied.emit(name);
    });
  }

}
