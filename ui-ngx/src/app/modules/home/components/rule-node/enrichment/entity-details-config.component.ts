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
  styleUrls: []
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
