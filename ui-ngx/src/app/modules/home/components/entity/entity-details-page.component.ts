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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  HostBinding,
  Injector,
  OnDestroy,
  OnInit
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { ActivatedRoute, Router } from '@angular/router';
import { FormGroup } from '@angular/forms';
import { AssetId } from '@shared/models/id/asset-id';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, mergeDeep } from '@core/utils';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { DialogService } from '@core/services/dialog.service';

@Component({
  selector: 'tb-entity-details-page',
  templateUrl: './entity-details-page.component.html',
  styleUrls: ['./entity-details-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDetailsPageComponent extends EntityDetailsPanelComponent implements OnInit, OnDestroy {

  headerTitle: string;
  headerSubtitle: string;

  isReadOnly = false;

  set entitiesTableConfig(entitiesTableConfig: EntityTableConfig<BaseData<HasId>>) {
    if (this.entitiesTableConfigValue !== entitiesTableConfig) {
      this.entitiesTableConfigValue = entitiesTableConfig;
      if (this.entitiesTableConfigValue) {
        this.isEdit = false;
        this.entity = null;
      }
    }
  }

  get entitiesTableConfig(): EntityTableConfig<BaseData<HasId>> {
    return this.entitiesTableConfigValue;
  }

  @HostBinding('class') 'tb-absolute-fill';

  constructor(private route: ActivatedRoute,
              private router: Router,
              protected injector: Injector,
              protected cd: ChangeDetectorRef,
              protected componentFactoryResolver: ComponentFactoryResolver,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private dialogService: DialogService,
              protected store: Store<AppState>) {
    super(store, injector, cd, componentFactoryResolver);
    this.entitiesTableConfig = this.route.snapshot.data.entitiesTableConfig;
  }

  ngOnInit() {
    this.headerSubtitle = '';
    this.route.paramMap.subscribe( paramMap => {
      this.entityId = new AssetId(paramMap.get('entityId'));
    });
    this.headerSubtitle = this.translate.instant(this.entitiesTableConfig.entityTranslations.details);
    super.init();
    this.entityComponent.isDetailsPage = true;
    this.subscriptions.push(this.entityAction.subscribe((action) => {
      if (action.action === 'delete') {
        this.deleteEntity(action.event, action.entity);
      }
    }));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  reload(): void {
    this.isEdit = false;
    this.entitiesTableConfig.loadEntity(this.currentEntityId).subscribe(
      (entity) => {
        this.entity = entity;
        this.broadcast.broadcast('updateBreadcrumb');
        this.isReadOnly = this.entitiesTableConfig.detailsReadonly(entity);
        this.headerTitle = this.entitiesTableConfig.entityTitle(entity);
        this.entityComponent.entity = entity;
        this.entityComponent.isEdit = false;
        if (this.entityTabsComponent) {
          this.entityTabsComponent.entity = entity;
        }
      }
    );
  }

  onToggleDetailsEditMode() {
    if (this.isEdit) {
      this.entityComponent.entity = this.entity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.entity;
      }
      this.isEdit = !this.isEdit;
    } else {
      this.isEdit = !this.isEdit;
      this.editingEntity = deepClone(this.entity);
      this.entityComponent.entity = this.editingEntity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.editingEntity;
      }
      if (this.entitiesTableConfig.hideDetailsTabsOnEdit) {
        this.selectedTab = 0;
      }
    }
  }

  onApplyDetails() {
    if (this.detailsForm && this.detailsForm.valid) {
      const editingEntity = {...this.editingEntity, ...this.detailsForm.getRawValue()};
      if (this.detailsForm.hasOwnProperty('additionalInfo')) {
        editingEntity.additionalInfo =
          mergeDeep((this.editingEntity as any).additionalInfo, this.detailsForm.getRawValue()?.additionalInfo);
      }
      this.entitiesTableConfig.saveEntity(editingEntity, this.editingEntity).subscribe(
        (entity) => {
          this.entity = entity;
          this.entityComponent.entity = entity;
          if (this.entityTabsComponent) {
            this.entityTabsComponent.entity = entity;
          }
          this.isEdit = false;
        }
      );
    }
  }

  confirmForm(): FormGroup {
    return this.detailsForm;
  }

  private deleteEntity($event: Event, entity: BaseData<HasId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.entitiesTableConfig.deleteEntityTitle(entity),
      this.entitiesTableConfig.deleteEntityContent(entity),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.entitiesTableConfig.deleteEntity(entity.id).subscribe(
          () => {
            this.router.navigate(['../'], {relativeTo: this.route});
          }
        );
      }
    });
  }
}
