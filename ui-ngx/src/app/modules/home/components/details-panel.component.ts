///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'tb-details-panel',
  templateUrl: './details-panel.component.html',
  styleUrls: ['./details-panel.component.scss']
})
export class DetailsPanelComponent extends PageComponent {

  @Input() headerHeightPx = 100;
  @Input() headerTitle = '';
  @Input() headerSubtitle = '';
  @Input() isReadOnly = false;
  @Input() isAlwaysEdit = false;

  theFormValue: FormGroup;

  @Input()
  set theForm(value: FormGroup) {
    this.theFormValue = value;
  }

  get theForm(): FormGroup {
    return this.theFormValue;
  }

  @Output()
  closeDetails = new EventEmitter<void>();
  @Output()
  toggleDetailsEditMode = new EventEmitter<boolean>();
  @Output()
  applyDetails = new EventEmitter<void>();

  isEditValue = false;

  @Output()
  isEditChange = new EventEmitter<boolean>();

  @Input()
  get isEdit() {
    return this.isAlwaysEdit || this.isEditValue;
  }

  set isEdit(val: boolean) {
    this.isEditValue = val;
    this.isEditChange.emit(this.isEditValue);
  }


  constructor(protected store: Store<AppState>) {
    super(store);
  }

  onCloseDetails() {
    this.closeDetails.emit();
  }

  onToggleDetailsEditMode() {
    if (!this.isAlwaysEdit) {
      this.isEdit = !this.isEdit;
    }
    this.toggleDetailsEditMode.emit(this.isEditValue);
  }

  onApplyDetails() {
    if (this.theForm && this.theForm.valid) {
      this.applyDetails.emit();
    }
  }

}
