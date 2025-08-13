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

import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { DataToFetch, dataToFetchTranslations, FetchTo } from '../rule-node-config.models';

@Component({
  selector: 'tb-enrichment-node-tenant-attributes-config',
  templateUrl: './tenant-attributes-config.component.html',
  styleUrls: ['./tenant-attributes-config.component.scss']
})
export class TenantAttributesConfigComponent extends RuleNodeConfigurationComponent {

  tenantAttributesConfigForm: FormGroup;
  public fetchToData = [];

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const key of dataToFetchTranslations.keys()) {
      if (key !== DataToFetch.FIELDS) {
        this.fetchToData.push({
          value: key,
          name: this.translate.instant(dataToFetchTranslations.get(key as DataToFetch))
        });
      }
    }
  }

  protected configForm(): FormGroup {
    return this.tenantAttributesConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    let dataToFetch: DataToFetch;
    if (isDefinedAndNotNull(configuration?.telemetry)) {
      dataToFetch = configuration.telemetry ? DataToFetch.LATEST_TELEMETRY : DataToFetch.ATTRIBUTES;
    } else {
      dataToFetch = isDefinedAndNotNull(configuration?.dataToFetch) ? configuration.dataToFetch : DataToFetch.ATTRIBUTES;
    }

    let dataMapping;
    if (isDefinedAndNotNull(configuration?.attrMapping)) {
      dataMapping = configuration.attrMapping;
    } else {
      dataMapping = isDefinedAndNotNull(configuration?.dataMapping) ? configuration.dataMapping : null;
    }

    return {
      dataToFetch,
      dataMapping,
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA
    };
  }

  public selectTranslation(latestTelemetryTranslation: string, attributesTranslation: string) {
    if (this.tenantAttributesConfigForm.get('dataToFetch').value === DataToFetch.LATEST_TELEMETRY) {
      return latestTelemetryTranslation;
    } else {
      return attributesTranslation;
    }
  }


  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.tenantAttributesConfigForm = this.fb.group({
      dataToFetch: [configuration.dataToFetch, []],
      dataMapping: [configuration.dataMapping, [Validators.required]],
      fetchTo: [configuration.fetchTo, []]
    });
  }

  protected readonly DataToFetch = DataToFetch;
}
