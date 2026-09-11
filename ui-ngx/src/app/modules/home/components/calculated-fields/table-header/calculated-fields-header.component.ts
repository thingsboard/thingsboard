// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
