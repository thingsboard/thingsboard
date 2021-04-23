///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { Firmware, FirmwareInfo } from '@shared/models/firmware.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { FirmwareService } from '@core/http/firmware.service';
import { PageLink } from '@shared/models/page/page-link';
import { FirmwaresComponent } from '@home/pages/firmware/firmwares.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DeviceInfo } from '@shared/models/device.models';

@Injectable()
export class FirmwareTableConfigResolve implements Resolve<EntityTableConfig<Firmware, PageLink, FirmwareInfo>> {

  private readonly config: EntityTableConfig<Firmware, PageLink, FirmwareInfo> = new EntityTableConfig<Firmware, PageLink, FirmwareInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private firmwareService: FirmwareService) {
    this.config.entityType = EntityType.FIRMWARE;
    this.config.entityComponent = FirmwaresComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.FIRMWARE);
    this.config.entityResources = entityTypeResources.get(EntityType.FIRMWARE);

    this.config.entityTitle = (firmware) => firmware ? firmware.title : '';

    this.config.columns.push(
      new DateEntityTableColumn<FirmwareInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<FirmwareInfo>('title', 'firmware.title', '50%'),
      new EntityTableColumn<FirmwareInfo>('version', 'firmware.version', '50%')
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('firmware.export'),
        icon: 'file_download',
        isEnabled: (firmware) => firmware.hasData,
        onAction: ($event, entity) => this.exportFirmware($event, entity)
      }
    );

    this.config.deleteEntityTitle = firmware => this.translate.instant('firmware.delete-firmware-title',
      { firmwareTitle: firmware.title });
    this.config.deleteEntityContent = () => this.translate.instant('firmware.delete-firmware-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('firmware.delete-firmwares-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('firmware.delete-firmwares-text');

    this.config.entitiesFetchFunction = pageLink => this.firmwareService.getFirmwares(pageLink);
    this.config.loadEntity = id => this.firmwareService.getFirmwareInfo(id.id);
    this.config.saveEntity = firmware => this.firmwareService.saveFirmware(firmware);
    this.config.deleteEntity = id => this.firmwareService.deleteFirmware(id.id);

    this.config.onEntityAction = action => this.onFirmwareAction(action);
  }

  resolve(): EntityTableConfig<Firmware, PageLink, FirmwareInfo> {
    this.config.tableTitle = this.translate.instant('firmware.firmware');
    return this.config;
  }

  exportFirmware($event: Event, firmware: FirmwareInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.firmwareService.downloadFirmware(firmware.id.id).subscribe();
  }

  onFirmwareAction(action: EntityAction<FirmwareInfo>): boolean {
    switch (action.action) {
      case 'uploadFirmware':
        this.exportFirmware(action.event, action.entity);
        return true;
    }
    return false;
  }

}
