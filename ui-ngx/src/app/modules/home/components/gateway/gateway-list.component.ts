///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import {Component, OnInit} from '@angular/core';
import {UtilsService} from '@core/services/utils.service';
import {TranslateService} from '@ngx-translate/core';
import {DeviceService} from '@core/http/device.service';
import {AttributeService} from '@core/http/attribute.service';
import {GatewayListTableConfig} from "@home/components/gateway/gateway-list-table-config";
import {DatePipe} from "@angular/common";
import {MatDialog} from "@angular/material/dialog";
import {Store} from "@ngrx/store";
import {AppState} from "@core/core.state";
import {EntityService} from "@core/http/entity.service";

@Component({
  selector: 'tb-gateway-list',
  templateUrl: './gateway-list.component.html',
  styleUrls: ['./gateway-list.component.scss']
})


export class GatewayListComponent implements OnInit {
  gatewayListTableConfig: GatewayListTableConfig;

  constructor(
    protected store: Store<AppState>,
    private utils: UtilsService,
    private translate: TranslateService,
    private datePipe: DatePipe,
    private deviceService: DeviceService,
    private entityService: EntityService,
    private attributeService: AttributeService,
    private dialog: MatDialog,
  ) {
  }

  ngOnInit(): void {
    this.gatewayListTableConfig = new GatewayListTableConfig(
      this.store,
      this.deviceService,
      this.attributeService,
      this.entityService,
      this.datePipe,
      this.translate,
      this.utils,
      this.dialog
    );
  }
}
