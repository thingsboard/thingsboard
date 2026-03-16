///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { OAuth2Client, OAuth2ClientInfo, platformTypeTranslations } from '@shared/models/oauth2.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { OAuth2Service } from '@core/http/oauth2.service';
import { ClientComponent } from '@home/pages/admin/oauth2/clients/client.component';
import { ClientTableHeaderComponent } from '@home/pages/admin/oauth2/clients/client-table-header.component';
import { Direction } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';

@Injectable()
export class ClientsTableConfigResolver  {

  private readonly config: EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo> =
    new EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private oauth2Service: OAuth2Service) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.OAUTH2_CLIENT;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.OAUTH2_CLIENT);
    this.config.entityResources = entityTypeResources.get(EntityType.OAUTH2_CLIENT);
    this.config.entityComponent = ClientComponent;
    this.config.headerComponent = ClientTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<OAuth2ClientInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<OAuth2ClientInfo>('title', 'admin.oauth2.title', '350px'),
      new EntityTableColumn<OAuth2ClientInfo>('platforms', 'admin.oauth2.allowed-platforms', '100%',
        (clientInfo) => clientInfo.platforms && clientInfo.platforms.length ?
          clientInfo.platforms.map(platform => this.translate.instant(platformTypeTranslations.get(platform))).join(', ') :
          this.translate.instant('admin.oauth2.all-platforms'), () => ({}), false)
    );

    this.config.deleteEntityTitle = (client) => this.translate.instant('admin.oauth2.delete-client-title', {clientName: client.title});
    this.config.deleteEntityContent = () => this.translate.instant('admin.oauth2.delete-client-text');
    this.config.entitiesFetchFunction = pageLink => this.oauth2Service.findTenantOAuth2ClientInfos(pageLink);
    this.config.loadEntity = id => this.oauth2Service.getOAuth2ClientById(id.id);
    this.config.saveEntity = client => this.oauth2Service.saveOAuth2Client(client);
    this.config.deleteEntity = id => this.oauth2Service.deleteOauth2Client(id.id);
  }

  resolve(): EntityTableConfig<OAuth2Client, PageLink, OAuth2ClientInfo> {
    return this.config;
  }

}
