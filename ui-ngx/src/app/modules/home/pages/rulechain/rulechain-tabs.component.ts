// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { RuleChain } from '@shared/models/rule-chain.models';

@Component({
    selector: 'tb-rulechain-tabs',
    templateUrl: './rulechain-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class RuleChainTabsComponent extends EntityTabsComponent<RuleChain> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
