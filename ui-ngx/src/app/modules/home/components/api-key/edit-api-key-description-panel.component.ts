///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ApiKeyService } from '@core/http/api-key.service';

@Component({
  selector: 'tb-edit-api-key-description-panel',
  templateUrl: './edit-api-key-description-panel.component.html',
  styleUrls: ['./edit-api-key-description-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class EditApiKeyDescriptionPanelComponent implements OnInit {

  @Input()
  apiKeyId: string;

  @Input()
  description: string;

  @Output()
  descriptionApplied = new EventEmitter<string>();

  descriptionFormControl = this.fb.control<string>(null);

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
