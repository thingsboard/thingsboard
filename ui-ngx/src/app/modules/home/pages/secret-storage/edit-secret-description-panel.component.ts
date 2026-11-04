// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { SecretStorageService } from '@core/http/secret-storage.service';

@Component({
    selector: 'tb-edit-secret-description-panel',
    templateUrl: './edit-secret-description-panel.component.html',
    styleUrls: ['./edit-secret-description-panel.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditSecretDescriptionPanelComponent implements OnInit {

  @Input()
  secretId: string;

  @Input()
  description: string;

  @Output()
  descriptionApplied = new EventEmitter<string>();

  descriptionFormControl = this.fb.control<string>(null);

  constructor(private fb: FormBuilder,
              private popover: TbPopoverComponent<EditSecretDescriptionPanelComponent>,
              private secretStorageService: SecretStorageService) {}

  ngOnInit(): void {
    this.descriptionFormControl.setValue(this.description, {emitEvent: false});
  }

  cancel() {
    this.popover.hide();
  }

  applyDescription() {
    const description = this.descriptionFormControl.value.trim();
    this.secretStorageService.updateSecretDescription(this.secretId, description).subscribe(() => {
      this.descriptionApplied.emit(description);
    });
  }

}
