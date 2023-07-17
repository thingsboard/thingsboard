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

import { Component, Inject, NgZone, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DeviceService } from '@core/http/device.service';
import { FormBuilder } from '@angular/forms';
import {
  AttributeData,
  AttributeScope,
  AttributesSubscriptionCmd,
  LatestTelemetry,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { selectPersistDeviceStateToTelemetry } from '@core/auth/auth.selectors';
import { take } from 'rxjs/operators';
import {
  BasicTransportType,
  DeviceTransportType,
  deviceTransportTypeTranslationMap,
  NetworkTransportType
} from '@shared/models/device.models';
import { UserSettingsService } from '@core/http/user-settings.service';
import { ActionPreferencesUpdateUserSettings } from '@core/auth/auth.actions';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

export interface DeviceCheckConnectivityDialogData {
  deviceId: EntityId;
  afterAdd: boolean;
}
@Component({
  selector: 'tb-device-check-connectivity-dialog',
  templateUrl: './device-check-connectivity-dialog.component.html',
  styleUrls: ['./device-check-connectivity-dialog.component.scss']
})
export class DeviceCheckConnectivityDialogComponent extends
  DialogComponent<DeviceCheckConnectivityDialogComponent> implements OnInit, OnDestroy {

  loadedCommand = false;

  status: boolean;

  latestTelemetry: Array<AttributeData> = [];

  commands: {[key: string]: string};

  allowTransportType = new Set<NetworkTransportType>();
  selectTransportType: NetworkTransportType;

  BasicTransportType = BasicTransportType;
  DeviceTransportType = DeviceTransportType;
  deviceTransportTypeTranslationMap = deviceTransportTypeTranslationMap;

  showDontShowAgain: boolean;
  dialogTitle: string;
  closeButtonLabel: string;

  notShowAgain = false;

  private telemetrySubscriber: TelemetrySubscriber;

  private currentTime = Date.now();

  private transportTypes = [...Object.keys(BasicTransportType), ...Object.keys(DeviceTransportType)] as Array<NetworkTransportType>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: DeviceCheckConnectivityDialogData,
              public dialogRef: MatDialogRef<DeviceCheckConnectivityDialogComponent>,
              private fb: FormBuilder,
              private deviceService: DeviceService,
              private telemetryWsService: TelemetryWebsocketService,
              private userSettingsService: UserSettingsService,
              private zone: NgZone) {
    super(store, router, dialogRef);

    if (this.data.afterAdd) {
      this.dialogTitle = 'device.connectivity.device-created-check-connectivity';
      this.closeButtonLabel = 'action.skip';
      this.showDontShowAgain = true;
    } else {
      this.dialogTitle = 'device.connectivity.check-connectivity';
      this.closeButtonLabel = 'action.close';
      this.showDontShowAgain = false;
    }
  }

  ngOnInit() {
    this.loadCommands();
    this.subscribeToLatestTelemetry();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.telemetrySubscriber?.complete();
    this.telemetrySubscriber?.unsubscribe();
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesUpdateUserSettings({ notDisplayConnectivityAfterAddDevice: true }));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  createMarkDownCommand(command: string): string {
    return '```bash\n' +
            command +
            '{:copy-code}\n' +
            '```';
  }

  private loadCommands() {
    this.deviceService.getDevicePublishTelemetryCommands(this.data.deviceId.id).subscribe(
      commands => {
        this.commands = commands;
        const commandsProtocols = Object.keys(commands);
        this.transportTypes.forEach(transport => {
          const findCommand = commandsProtocols.find(item => item.toUpperCase().startsWith(transport));
          if (findCommand) {
            this.allowTransportType.add(transport);
          }
        });
        this.selectTransportType = this.allowTransportType.values().next().value;
        this.loadedCommand = true;
      }
    );
  }

  private subscribeToLatestTelemetry() {
    this.store.pipe(select(selectPersistDeviceStateToTelemetry)).pipe(
      take(1)
    ).subscribe(persistToTelemetry => {
      this.telemetrySubscriber = TelemetrySubscriber.createEntityAttributesSubscription(
        this.telemetryWsService, this.data.deviceId, LatestTelemetry.LATEST_TELEMETRY, this.zone);
      if (!persistToTelemetry) {
        const subscriptionCommand = new AttributesSubscriptionCmd();
        subscriptionCommand.entityType = this.data.deviceId.entityType as EntityType;
        subscriptionCommand.entityId = this.data.deviceId.id;
        subscriptionCommand.scope = AttributeScope.SERVER_SCOPE;
        subscriptionCommand.keys = 'active';
        this.telemetrySubscriber.subscriptionCommands.push(subscriptionCommand);
      }

      this.telemetrySubscriber.subscribe();
      this.telemetrySubscriber.attributeData$().subscribe(
        (data) => {
          this.latestTelemetry = data.reduce<Array<AttributeData>>((accumulator, item) => {
            if (item.key === 'active') {
              this.status = coerceBooleanProperty(item.value);
            } else if (item.lastUpdateTs > this.currentTime) {
              accumulator.push(item);
            }
            return accumulator;
          }, []);
        }
      );
    });
  }

}
