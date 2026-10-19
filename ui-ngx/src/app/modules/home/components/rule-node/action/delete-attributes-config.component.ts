// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { MatChipGrid, MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-action-node-delete-attributes-config',
    templateUrl: './delete-attributes-config.component.html',
    styleUrls: [],
    standalone: false
})
export class DeleteAttributesConfigComponent extends RuleNodeConfigurationComponent {
  @ViewChild('attributeChipList') attributeChipList: MatChipGrid;

  deleteAttributesConfigForm: UntypedFormGroup;
  attributeScopeMap = AttributeScope;
  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;
  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.deleteAttributesConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deleteAttributesConfigForm = this.fb.group({
      scope: [configuration ? configuration.scope : null, [Validators.required]],
      keys: [configuration ? configuration.keys : null, [Validators.required]],
      sendAttributesDeletedNotification: [configuration ? configuration.sendAttributesDeletedNotification : false, []],
      notifyDevice: [configuration ? configuration.notifyDevice : false, []]
    });

    this.deleteAttributesConfigForm.get('scope').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      if (value !== AttributeScope.SHARED_SCOPE) {
        this.deleteAttributesConfigForm.get('notifyDevice').patchValue(false, {emitEvent: false});
      }
    });
  }

  removeKey(key: string): void {
    const keys: string[] = this.deleteAttributesConfigForm.get('keys').value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.deleteAttributesConfigForm.get('keys').patchValue(keys, {emitEvent: true});
    }
  }

  addKey(event: MatChipInputEvent): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.deleteAttributesConfigForm.get('keys').value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.deleteAttributesConfigForm.get('keys').patchValue(keys, {emitEvent: true});
      }
    }
    if (input) {
      input.value = '';
    }
  }
}
