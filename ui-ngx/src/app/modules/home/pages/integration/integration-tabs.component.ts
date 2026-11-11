// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { Integration, IntegrationInfo } from '@shared/models/integration.models';
import { PageLink } from '@shared/models/page/page-link';
import { DebugEventType, EventType } from '@shared/models/event.models';

@Component({
    selector: 'tb-integration-tabs',
    templateUrl: './integration-tabs.component.html',
    standalone: false
})
export class IntegrationTabsComponent extends EntityTabsComponent<Integration, PageLink, IntegrationInfo> {

  private defaultEventTypeValue: DebugEventType | EventType  = DebugEventType.DEBUG_INTEGRATION;
  get defaultEventType(): DebugEventType | EventType {
    return this.defaultEventTypeValue;
  }

  @Input()
  set defaultEventType(value: DebugEventType | EventType) {
    this.defaultEventTypeValue = value || DebugEventType.DEBUG_INTEGRATION;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
