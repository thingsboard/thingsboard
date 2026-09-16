// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { AppState } from '@app/core/public-api';
import { AuditLog, AuditLogFilter, TimePageLink } from '@app/shared/public-api';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';

@Component({
    selector: 'tb-audit-log-header',
    templateUrl: './audit-log-header.component.html',
    styles: ``,
    standalone: false
})
export class AuditLogHeaderComponent extends EntityTableHeaderComponent<AuditLog, TimePageLink> {

  constructor(protected store: Store<AppState>) {
    super(store)
  }

  auditLogFiltersChanged(auditLogFilter: AuditLogFilter) {
    this.entitiesTableConfig.componentsData.auditLogFilter = auditLogFilter;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
