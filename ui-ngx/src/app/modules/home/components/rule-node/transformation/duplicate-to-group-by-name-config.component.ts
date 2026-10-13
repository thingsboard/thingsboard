// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { entityGroupTypes } from '@app/shared/models/entity-group.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-transformation-node-duplicate-to-group-by-name-config',
    templateUrl: './duplicate-to-group-by-name-config.component.html',
    styleUrls: [],
    standalone: false
})
export class DuplicateToGroupByNameConfigComponent extends RuleNodeConfigurationComponent {

  duplicateToGroupByNameConfigForm: UntypedFormGroup;

  entityGroupTypesList = entityGroupTypes;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.duplicateToGroupByNameConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.duplicateToGroupByNameConfigForm = this.fb.group({
      searchEntityGroupForTenantOnly: [configuration ? configuration.searchEntityGroupForTenantOnly : false, []],
      considerMessageOriginatorAsAGroupOwner: [configuration ? configuration.considerMessageOriginatorAsAGroupOwner : true, []],
      groupType: [configuration ? configuration.groupType : null, [Validators.required]],
      groupName: [configuration ? configuration.groupName : null, [Validators.required]]
    });

    this.duplicateToGroupByNameConfigForm.get('searchEntityGroupForTenantOnly').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      const considerMessageOriginatorAsAGroupOwner = this.duplicateToGroupByNameConfigForm.get('considerMessageOriginatorAsAGroupOwner');
      if (value) {
        considerMessageOriginatorAsAGroupOwner.setValue(false);
        considerMessageOriginatorAsAGroupOwner.disable({emitEvent: false});
      } else {
        considerMessageOriginatorAsAGroupOwner.enable({emitEvent: false});
      }
    });
  }

  protected updateValidators() {
    if (this.duplicateToGroupByNameConfigForm.get('searchEntityGroupForTenantOnly').value === true) {
      this.duplicateToGroupByNameConfigForm.get('considerMessageOriginatorAsAGroupOwner').disable({emitEvent: false});
    }
  }

}
