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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { CalculatedField, CalculatedFieldsQuery } from "@shared/models/calculated-field.models";
import { CalculatedFieldsTableConfig } from '@home/components/calculated-fields/calculated-fields-table-config';

@Component({
    selector: 'tb-calculated-fields-table-header',
    templateUrl: './calculated-fields-header.component.html',
    styleUrls: ['./calculated-fields-header.component.scss'],
    standalone: false
})
export class CalculatedFieldsHeaderComponent extends EntityTableHeaderComponent<CalculatedField> {

  get calculatedFieldsTableConfig(): CalculatedFieldsTableConfig {
    return this.entitiesTableConfig as CalculatedFieldsTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  calculatedFieldsFilterChanged(calculatedFieldFilterConfig: CalculatedFieldsQuery) {
    this.calculatedFieldsTableConfig.calculatedFieldFilterConfig = calculatedFieldFilterConfig;
    this.calculatedFieldsTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
