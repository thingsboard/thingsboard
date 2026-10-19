// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ApiKeyService } from '@core/http/api-key.service';

@Component({
    selector: 'tb-edit-api-key-description-panel',
    templateUrl: './edit-api-key-description-panel.component.html',
    styleUrls: ['./edit-api-key-description-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditApiKeyDescriptionPanelComponent implements OnInit {

  @Input()
  apiKeyId: string;

  @Input()
  description: string;

  @Output()
  descriptionApplied = new EventEmitter<string>();

  descriptionFormControl = this.fb.control<string>(null, Validators.required);

  constructor(private fb: FormBuilder,
              private popover: TbPopoverComponent<EditApiKeyDescriptionPanelComponent>,
              private apiKeyService: ApiKeyService) {}

  ngOnInit(): void {
    this.descriptionFormControl.setValue(this.description, {emitEvent: false});
  }

  cancel() {
    this.popover.hide();
  }

  applyDescription() {
    const description = this.descriptionFormControl.value.trim();
    this.apiKeyService.updateApiKeyDescription(this.apiKeyId, description).subscribe(() => {
      this.descriptionApplied.emit(description);
    });
  }

}
