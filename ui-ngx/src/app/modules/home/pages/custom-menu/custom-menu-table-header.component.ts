// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { CustomMenuInfo } from '@shared/models/custom-menu.models';

@Component({
    selector: 'tb-custom-menu-table-header',
    templateUrl: './custom-menu-table-header.component.html',
    styleUrls: ['./custom-menu-table-header.component.scss'],
    standalone: false
})
export class CustomMenuTableHeaderComponent extends EntityTableHeaderComponent<CustomMenuInfo> implements OnInit {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
