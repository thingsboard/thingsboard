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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-details-panel',
  templateUrl: './details-panel.component.html',
  styleUrls: ['./details-panel.component.scss']
})
export class DetailsPanelComponent extends PageComponent implements OnDestroy {

  @Input() headerHeightPx = 100;
  @Input() headerTitle = '';
  @Input() headerSubtitle = '';
  @Input() isReadOnly = false;
  @Input() isAlwaysEdit = false;
  @Input() isShowSearch = false;
  @Input() backgroundColor = '#FFF';

  private theFormValue: FormGroup;
  private formSubscription: Subscription = null;

  @Input()
  set theForm(value: FormGroup) {
    if (this.theFormValue !== value) {
      if (this.formSubscription !== null) {
        this.formSubscription.unsubscribe();
        this.formSubscription = null;
      }
      this.theFormValue = value;
      if (this.theFormValue !== null) {
        this.formSubscription = this.theFormValue.valueChanges.subscribe(() => this.cd.detectChanges());
      }
    }
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
  @Output()
  closeSearch = new EventEmitter<void>();

  isEditValue = false;
  showSearchPane = false;

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


  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnDestroy() {
    if (this.formSubscription !== null) {
      this.formSubscription.unsubscribe();
    }
    super.ngOnDestroy();
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

  onToggleSearch() {
    this.showSearchPane = !this.showSearchPane;
    if (!this.showSearchPane) {
      this.closeSearch.emit();
    }
  }
}
