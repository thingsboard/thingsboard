///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { AfterViewInit, ChangeDetectorRef, Component,  Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from "@shared/components/page.component";
import { DialogService } from '@app/core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { ContentType } from '@shared/models/constants';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from "@shared/components/dialog/json-object-edit-dialog.component";


@Component({
  selector: 'tb-gateway-service-rpc',
  templateUrl: './gateway-service-rpc.component.html',
  styleUrls: ['./gateway-service-rpc.component.scss']
})
export class GatewayServiceRPCComponent extends PageComponent implements AfterViewInit {

  @Input()
  ctx: WidgetContext;

  contentTypes = ContentType;

  @Input()
  dialogRef: MatDialogRef<any>;

  commandForm: FormGroup;

  isConnector: boolean;

  RPCCommands: Array<string> = [
    "Ping",
    "Stats",
    "Devices",
    "Update",
    "Version",
    "Restart",
    "Reboot"
  ]


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              private cd: ChangeDetectorRef,
              public dialog: MatDialog) {
    super(store);
    this.commandForm = this.fb.group({
      command: [null,[Validators.required]],
      time: [60, [Validators.required]],
      params: ["{}", [Validators.required]],
      result: [null]
    })


  }


  ngAfterViewInit() {
    this.isConnector = this.ctx.settings.isConnector;
    if (!this.isConnector) {
      this.commandForm.get('command').setValue(this.RPCCommands[0]);
    }
  }


  sendCommand() {
    const formValues = this.commandForm.value;
    this.ctx.controlApi.sendTwoWayCommand(formValues.command.toLowerCase(), {},formValues.time).subscribe(resp=>{
      console.log(resp);
      this.commandForm.get('result').setValue(resp);
    },error => {
      console.log(error);
      this.commandForm.get('result').setValue(error.error);
    })
  }

  openEditJSONDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, object>(JsonObjectEditDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: JSON.parse(this.commandForm.get('params').value)
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.commandForm.get('params').setValue(JSON.stringify(res));
        }
      }
    );
  }

}
