// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import {
  EntityDetailsField,
  entityDetailsTranslations,
  FetchTo
} from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-enrichment-node-entity-details-config',
    templateUrl: './entity-details-config.component.html',
    styleUrls: [],
    standalone: false
})

export class EntityDetailsConfigComponent extends RuleNodeConfigurationComponent implements OnInit {

  entityDetailsConfigForm: FormGroup;

  public predefinedValues = [];

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
    for (const field of Object.keys(EntityDetailsField)) {
      this.predefinedValues.push({
        value: EntityDetailsField[field],
        name: this.translate.instant(entityDetailsTranslations.get(EntityDetailsField[field]))
      });
    }
  }

  ngOnInit() {
    super.ngOnInit();
  }

  protected configForm(): FormGroup {
    return this.entityDetailsConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let fetchTo: FetchTo;
    if (isDefinedAndNotNull(configuration?.addToMetadata)) {
      if (configuration.addToMetadata) {
        fetchTo = FetchTo.METADATA;
      } else {
        fetchTo = FetchTo.DATA;
      }
    } else {
      if (configuration?.fetchTo) {
        fetchTo = configuration.fetchTo;
      } else {
        fetchTo = FetchTo.DATA;
      }
    }

    return {
      detailsList: isDefinedAndNotNull(configuration?.detailsList) ? configuration.detailsList : null,
      fetchTo
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.entityDetailsConfigForm = this.fb.group({
      detailsList: [configuration.detailsList, [Validators.required]],
      fetchTo: [configuration.fetchTo, []]
    });
  }
}
