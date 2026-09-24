// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { EntityId } from "@shared/models/id/entity-id";
import { DeviceService } from '@core/http/device.service';
@Component({
    selector: 'tb-device-tabs',
    templateUrl: './device-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class DeviceTabsComponent extends EntityTabsComponent<DeviceInfo> {

  ownerId: EntityId;

  constructor(protected store: Store<AppState>,
    private deviceService: DeviceService) {

    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  resolveTabIndex(tab: string): number {
    if (tab === 'cf') {
      return 3;
    } else {
      return super.resolveTabIndex(tab);
    }
  }

  protected setEntity(entity: DeviceInfo) {
    this.ownerId = entity.customerId.id !== this.nullUid ? entity.customerId : entity.tenantId;
    super.setEntity(entity);
  }

pingDevice($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const id = this.entity.id.id;

    this.deviceService.pingDevice(id).subscribe(
      (res) => {
        const status = res.reachable ? 'Reachable' : 'Unreachable';
        const msg = `Device is ${status} (Last Seen: ${new Date(res.lastSeen).toLocaleString()})`;
        alert(msg);
      },
      (err) => {
        console.error(err);
        alert('Failed to ping device');
      }
    );
  }
}
