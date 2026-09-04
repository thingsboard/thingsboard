// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetTypeDetails } from '@shared/models/widget.models';

@Component({
    selector: 'tb-widget-type-tabs',
    templateUrl: './widget-type-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class WidgetTypeTabsComponent extends EntityTabsComponent<WidgetTypeDetails> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  isTenantWidgetType() {
    return this.entity && this.entity.tenantId.id !== NULL_UUID;
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
