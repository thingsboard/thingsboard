///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { EntityTableColumn, EntityTableConfig } from '../../models/entity/entities-table-config.models';
import { QueueInfo, ServiceType } from '../../../../shared/models/queue.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '../../../../core/core.state';
import { BroadcastService } from '../../../../core/services/broadcast.service';
import { CustomerService } from '../../../../core/http/customer.service';
import { DialogService } from '../../../../core/services/dialog.service';
import { HomeDialogsService } from '../../dialogs/home-dialogs.service';
import { map, mergeMap, take } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { EntityType, entityTypeResources, entityTypeTranslations } from '../../../../shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { QueueComponent } from './queue.component';
import { QueueService } from '../../../../core/http/queue.service';
import { selectAuthUser } from '../../../../core/auth/auth.selectors';
import { PageData } from '../../../../shared/models/page/page-data';
import { PageLink } from '../../../../shared/models/page/page-link';

@Injectable()
export class QueuesTableConfigResolver implements Resolve<EntityTableConfig<QueueInfo>> {

  readonly queueType = ServiceType.TB_RULE_ENGINE;

  private readonly config: EntityTableConfig<QueueInfo> = new EntityTableConfig<QueueInfo>();

  private allQueues: Observable<PageData<QueueInfo>>;
  private pageLink: PageLink = null;

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              private queueService: QueueService,
              private customerService: CustomerService,
              private dialogService: DialogService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService) {

    this.config.entityType = EntityType.QUEUE;
    this.config.entityComponent = QueueComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.QUEUE);
    this.config.entityResources = entityTypeResources.get(EntityType.QUEUE);

    this.config.deleteEntityTitle = queue => this.translate.instant('admin.delete-queue-title', {queueName: queue.name});
    this.config.deleteEntityContent = () => this.translate.instant('admin.delete-queue-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('admin.delete-queue-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('admin.delete-queue-text');
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<QueueInfo>> {
    this.config.componentsData = {
      queueType: this.queueType
    };

    return this.store.pipe(select(selectAuthUser), take(1)).pipe(
      map(() => {
        this.config.tableTitle = this.translate.instant('admin.queues');
        this.config.columns = this.configureColumns();
        this.configureEntityFunctions();
        return this.config;
      })
    );
  }

  fetchFunction(pageLink: PageLink, queueType: ServiceType): Observable<PageData<QueueInfo>> {
    this.pageLink = pageLink;
    return this.getAllQueues(pageLink, queueType).pipe(
      map((data) => this.addInnerObjectPropsForColumns(pageLink.filterData(data.data)))
    );
  }

  addInnerObjectPropsForColumns(data: PageData<QueueInfo>): PageData<QueueInfo> {
    const modifiedData = Object.assign({}, data);
    modifiedData.data.forEach(item => {
      item.processingStrategyType = item.processingStrategy?.type;
      item.submitStrategyType = item.submitStrategy?.type;
    });
    return modifiedData;
  }

  getAllQueues(pageLink: PageLink, queueType: ServiceType): Observable<PageData<QueueInfo>> {
    if (!this.allQueues) {
      this.allQueues = this.queueService.getTenantQueuesByServiceType(pageLink, queueType);
    }
    return this.allQueues;
  }

  configureColumns(): Array<EntityTableColumn<QueueInfo>> {
    return [
      new EntityTableColumn<QueueInfo>('name', 'admin.queue-name', '25%'),
      new EntityTableColumn<QueueInfo>('partitions', 'admin.queue-partitions', '25%'),
      new EntityTableColumn<QueueInfo>('submitStrategyType', 'admin.queue-submit-strategy', '25%'),
      new EntityTableColumn<QueueInfo>('processingStrategyType', 'admin.queue-processing-strategy', '25%')
    ];
  }

  configureEntityFunctions(): void {
    this.config.entitiesFetchFunction = pageLink => this.fetchFunction(pageLink, this.queueType);
    this.config.loadEntity = id => this.getQueueInfo(this.pageLink, this.queueType, id.id);
    this.config.saveEntity = queue => this.queueService.saveQueue(queue, this.queueType).pipe(
      mergeMap((savedQueue) => this.getQueueInfo(this.pageLink, this.queueType, savedQueue.id.id)
      ));
    this.config.deleteEntity = id => this.queueService.deleteQueue(id.id);
  }

  getQueueInfo(pageLink: PageLink, queueType: ServiceType, id: string): Observable<QueueInfo> {
    return this.queueService.getTenantQueuesByServiceType(this.pageLink, this.queueType).pipe(
      map(data => {
        return data.data.find(queue => queue.id.id === id);
      }));
  }
}
