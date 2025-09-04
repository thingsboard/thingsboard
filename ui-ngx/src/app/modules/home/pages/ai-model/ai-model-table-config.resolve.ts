///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { AiModel, AiProviderTranslations } from '@shared/models/ai-model.models';
import { AiModelService } from '@core/http/ai-model.service';
import { AiModelTableHeaderComponent } from '@home/pages/ai-model/ai-model-table-header.component';
import { AIModelDialogComponent, AIModelDialogData } from '@home/components/ai-model/ai-model-dialog.component';
import { map } from 'rxjs/operators';

@Injectable()
export class AiModelsTableConfigResolver {

  private readonly config: EntityTableConfig<AiModel> = new EntityTableConfig<AiModel>();

  constructor(
    private datePipe: DatePipe,
    private aiModelService: AiModelService,
    private translate : TranslateService,
    private dialog: MatDialog
  ) {
    this.config.selectionEnabled = true;
    this.config.entityType = EntityType.AI_MODEL;
    this.config.addAsTextButton = true;
    this.config.rowPointer = true;
    this.config.detailsPanelEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.AI_MODEL);
    this.config.entityResources = entityTypeResources.get(EntityType.AI_MODEL);

    this.config.headerComponent = AiModelTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.addEntity = () => this.addModel(null, true);

    this.config.columns.push(
      new DateEntityTableColumn<AiModel>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<AiModel>('name', 'ai-models.name', '33%'),
      new EntityTableColumn<AiModel>('provider', 'ai-models.provider', '33%',
          entity => this.translate.instant(AiProviderTranslations.get(entity.configuration.provider))
      ),
      new EntityTableColumn<AiModel>('modelId', 'ai-models.model', '33%', entity => entity.configuration.modelId)
    )

    this.config.deleteEntityTitle = model => this.translate.instant('ai-models.delete-model-title', {modelName: model.name});
    this.config.deleteEntityContent = () => this.translate.instant('ai-models.delete-model-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('ai-models.delete-models-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('ai-models.delete-models-text');

    this.config.deleteEntity = id => this.aiModelService.deleteAiModel(id.id);

    this.config.entitiesFetchFunction = pageLink => this.aiModelService.getAiModels(pageLink);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.handleRowClick = ($event, model) => {
      this.editModel($event, model);
      return true;
    };
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<AiModel> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<AiModel>> {
    return [
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editModel($event, entity)
      }
    ];
  }

  private editModel($event, AIModel: AiModel): void {
    $event?.stopPropagation();
    this.addModel(AIModel, false).subscribe(res => res ? this.config.updateData() : null);
  }

  private addModel(AIModel: AiModel, isAdd = false): Observable<AiModel> {
    return this.dialog.open<AIModelDialogComponent, AIModelDialogData, AiModel>(AIModelDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        AIModel
      }
    }).afterClosed();
  }
}
