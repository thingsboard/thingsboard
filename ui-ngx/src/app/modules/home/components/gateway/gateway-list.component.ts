///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
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
    private attributeService: AttributeService,
    private dialog: MatDialog,
  ) {
  }

  ngOnInit(): void {
    this.gatewayListTableConfig = new GatewayListTableConfig(
      this.store,
      this.deviceService,
      this.attributeService,
      this.datePipe,
      this.translate,
      this.utils,
      this.dialog
    );
  }
}
