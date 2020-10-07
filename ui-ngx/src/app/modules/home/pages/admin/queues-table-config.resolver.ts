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
import {
  EntityTableColumn,
  EntityTableConfig
} from '../../models/entity/entities-table-config.models';
import { QueueInfo, ServiceType } from '../../../../shared/models/queue.models';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BroadcastService } from '@core/services/broadcast.service';
import { CustomerService } from '@core/http/customer.service';
import { DialogService } from '@core/services/dialog.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { map, mergeMap, take } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@app/shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { QueueComponent } from './queue.component';
import { QueueService } from '@core/http/queue.service';
import { selectAuthUser } from '@core/auth/auth.selectors';

@Injectable()
export class QueuesTableConfigResolver implements Resolve<EntityTableConfig<QueueInfo>> {

  readonly queueType = ServiceType.TB_RULE_ENGINE;

  private readonly config: EntityTableConfig<QueueInfo> = new EntityTableConfig<QueueInfo>();

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

    this.config.deleteEntityTitle = queue => this.translate.instant('queue.delete-queue-title', {queueName: queue.name});
    this.config.deleteEntityContent = () => this.translate.instant('queue.delete-queue-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('queue.delete-queues-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('queue.delete-queues-text');
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

  configureColumns(): Array<EntityTableColumn<QueueInfo>> {
    return [
      new EntityTableColumn<QueueInfo>('name', 'admin.queue-name', '25%'),
      new EntityTableColumn<QueueInfo>('partitions', 'admin.queue-partitions', '25%'),
      new EntityTableColumn<QueueInfo>('submitStrategy', 'admin.queue-submit-strategy', '25%',
        (entity: QueueInfo) => {
          return entity.submitStrategy.type;
        },
        () => ({}),
        false
      ),
      new EntityTableColumn<QueueInfo>('processingStrategy', 'admin.queue-processing-strategy', '25%',
        (entity: QueueInfo) => {
          return entity.processingStrategy.type;
        },
        () => ({}),
        false
      )
    ];
  }

  configureEntityFunctions(): void {
    this.config.entitiesFetchFunction = pageLink => this.queueService.getTenantQueuesByServiceType(pageLink, this.queueType);
    this.config.loadEntity = id => this.queueService.getQueueById(id.id);
    this.config.saveEntity = queue => this.queueService.saveQueue(this.addTopicForQueue(queue), this.queueType).pipe(
      mergeMap((savedQueue) => this.queueService.getQueueById(savedQueue.id.id)
      ));
    this.config.deleteEntity = id => this.queueService.deleteQueue(id.id);
    this.config.deleteEnabled = (queue) => queue && queue.name !== 'Main';
  }

  private addTopicForQueue(queue: QueueInfo): QueueInfo {
    const modifiedQueue = Object.assign({}, queue);
    modifiedQueue.topic = `tb_rule_engine.${queue.name}`;
    return modifiedQueue;
  }
}
