///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  Component,
  ComponentFactoryResolver,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@shared/components/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import {
  EntityType,
  EntityTypeResource,
  EntityTypeTranslation
} from '@shared/models/entity-type.models';
import { NgForm } from '@angular/forms';
import { EntityComponent } from '@shared/components/entity/entity.component';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { EntityAction } from '@shared/components/entity/entity-component.models';
import { Subscription } from 'rxjs';
// import { AuditLogMode } from '@shared/models/audit-log.models';

@Component({
  selector: 'tb-entity-details-panel',
  templateUrl: './entity-details-panel.component.html',
  styleUrls: ['./entity-details-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDetailsPanelComponent extends PageComponent implements OnInit, OnDestroy {

  @Input() entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;

  @Output()
  closeEntityDetails = new EventEmitter<void>();

  @Output()
  entityUpdated = new EventEmitter<BaseData<HasId>>();

  @Output()
  entityAction = new EventEmitter<EntityAction<BaseData<HasId>>>();

  entityComponent: EntityComponent<BaseData<HasId>>;
  detailsForm: NgForm;

  isEditValue = false;
  selectedTab = 0;

  entityTypes = EntityType;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  translations: EntityTypeTranslation;
  resources: EntityTypeResource;
  entity: BaseData<HasId>;

  private currentEntityId: HasId;
  private entityActionSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private componentFactoryResolver: ComponentFactoryResolver) {
    super(store);
  }

  @Input()
  set entityId(entityId: HasId) {
    if (entityId && entityId !== this.currentEntityId) {
      this.currentEntityId = entityId;
      this.reload();
    }
  }

  set isEdit(val: boolean) {
    this.isEditValue = val;
    this.entityComponent.isEdit = val;
  }

  get isEdit() {
    return this.isEditValue;
  }

  ngOnInit(): void {
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.buildEntityComponent();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.entityActionSubscription) {
      this.entityActionSubscription.unsubscribe();
    }
  }

  buildEntityComponent() {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.entitiesTableConfig.entityComponent);
    const viewContainerRef = this.entityDetailsFormAnchor.viewContainerRef;
    viewContainerRef.clear();
    const componentRef = viewContainerRef.createComponent(componentFactory);
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = this.isEdit;
    this.entityComponent.entitiesTableConfig = this.entitiesTableConfig;
    this.detailsForm = this.entityComponent.entityNgForm;
    this.entityActionSubscription = this.entityComponent.entityAction.subscribe((action) => {
      this.entityAction.emit(action);
    });
  }

  reload(): void {
    this.isEdit = false;
    this.entitiesTableConfig.loadEntity(this.currentEntityId).subscribe(
      (entity) => {
        this.entity = entity;
        this.entityComponent.entity = entity;
      }
    );
  }

  onCloseEntityDetails() {
    this.closeEntityDetails.emit();
  }

  onToggleEditMode(isEdit: boolean) {
    this.isEdit = isEdit;
    if (!this.isEdit) {
      this.entityComponent.entity = this.entity;
    } else {
      this.selectedTab = 0;
    }
  }

  saveEntity() {
    if (this.detailsForm.valid) {
      const editingEntity = {...this.entity, ...this.entityComponent.entityFormValue()};
      this.entitiesTableConfig.saveEntity(editingEntity).subscribe(
        (entity) => {
          this.entity = entity;
          this.entityComponent.entity = entity;
          this.isEdit = false;
          this.entityUpdated.emit(this.entity);
        }
      );
    }
  }

}
