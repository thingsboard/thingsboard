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
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  ComponentRef,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityType, EntityTypeResource, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { FormGroup } from '@angular/forms';
import { EntityComponent } from './entity.component';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Subscription } from 'rxjs';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';
import { deepClone, mergeDeep } from '@core/utils';

@Component({
  selector: 'tb-entity-details-panel',
  templateUrl: './entity-details-panel.component.html',
  styleUrls: ['./entity-details-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDetailsPanelComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Output()
  closeEntityDetails = new EventEmitter<void>();

  @Output()
  entityUpdated = new EventEmitter<BaseData<HasId>>();

  @Output()
  entityAction = new EventEmitter<EntityAction<BaseData<HasId>>>();

  entityComponentRef: ComponentRef<EntityComponent<BaseData<HasId>>>;
  entityComponent: EntityComponent<BaseData<HasId>>;

  entityTabsComponentRef: ComponentRef<EntityTabsComponent<BaseData<HasId>>>;
  entityTabsComponent: EntityTabsComponent<BaseData<HasId>>;
  detailsForm: FormGroup;

  entitiesTableConfigValue: EntityTableConfig<BaseData<HasId>>;
  isEditValue = false;
  selectedTab = 0;

  entityTypes = EntityType;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  @ViewChild('entityTabs', {static: true}) entityTabsAnchor: TbAnchorComponent;

  @ViewChild(MatTabGroup, {static: true}) matTabGroup: MatTabGroup;

  @ViewChildren(MatTab) inclusiveTabs: QueryList<MatTab>;

  translations: EntityTypeTranslation;
  resources: EntityTypeResource<BaseData<HasId>>;
  entity: BaseData<HasId>;
  editingEntity: BaseData<HasId>;

  private currentEntityId: HasId;
  private subscriptions: Subscription[] = [];
  private viewInited = false;
  private pendingTabs: MatTab[];

  constructor(protected store: Store<AppState>,
              private injector: Injector,
              private cd: ChangeDetectorRef,
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

  @Input()
  set entitiesTableConfig(entitiesTableConfig: EntityTableConfig<BaseData<HasId>>) {
    if (this.entitiesTableConfigValue !== entitiesTableConfig) {
      this.entitiesTableConfigValue = entitiesTableConfig;
      if (this.entitiesTableConfigValue) {
        this.currentEntityId = null;
        this.isEdit = false;
        this.entity = null;
        this.init();
      }
    }
  }

  get entitiesTableConfig(): EntityTableConfig<BaseData<HasId>> {
    return this.entitiesTableConfigValue;
  }

  set isEdit(val: boolean) {
    this.isEditValue = val;
    if (this.entityComponent) {
      this.entityComponent.isEdit = val;
    }
    if (this.entityTabsComponent) {
      this.entityTabsComponent.isEdit = val;
    }
  }

  get isEdit() {
    return this.isEditValue;
  }

  ngOnInit(): void {
    this.init();
  }

  private init() {
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.buildEntityComponent();
  }

  private clearSubscriptions() {
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.subscriptions.length = 0;
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.clearSubscriptions();
  }

  buildEntityComponent() {
    this.clearSubscriptions();
    if (this.entityComponentRef) {
      this.entityComponentRef.destroy();
      this.entityComponentRef = null;
    }
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.entitiesTableConfig.entityComponent);
    const viewContainerRef = this.entityDetailsFormAnchor.viewContainerRef;
    viewContainerRef.clear();
    const injector: Injector = Injector.create(
      {
        providers: [
          {
            provide: 'entity',
            useValue: this.entity
          },
          {
            provide: 'entitiesTableConfig',
            useValue: this.entitiesTableConfig
          }
        ],
        parent: this.injector
      }
    );
    this.entityComponentRef = viewContainerRef.createComponent(componentFactory, 0, injector);
    this.entityComponent = this.entityComponentRef.instance;
    this.entityComponent.isEdit = this.isEdit;
    this.detailsForm = this.entityComponent.entityForm;
    this.subscriptions.push(this.entityComponent.entityAction.subscribe((action) => {
      this.entityAction.emit(action);
    }));
    this.buildEntityTabsComponent();
    this.subscriptions.push(this.entityComponent.entityForm.valueChanges.subscribe(() => {
      this.cd.detectChanges();
    }));
  }

  buildEntityTabsComponent() {
    if (this.entityTabsComponentRef) {
      this.entityTabsComponentRef.destroy();
      this.entityTabsComponentRef = null;
    }
    const viewContainerRef = this.entityTabsAnchor.viewContainerRef;
    viewContainerRef.clear();
    this.entityTabsComponent = null;
    if (this.entitiesTableConfig.entityTabsComponent) {
      const componentTabsFactory = this.componentFactoryResolver.resolveComponentFactory(this.entitiesTableConfig.entityTabsComponent);
      this.entityTabsComponentRef = viewContainerRef.createComponent(componentTabsFactory);
      this.entityTabsComponent = this.entityTabsComponentRef.instance;
      this.entityTabsComponent.isEdit = this.isEdit;
      this.entityTabsComponent.entitiesTableConfig = this.entitiesTableConfig;
      this.entityTabsComponent.detailsForm = this.detailsForm;
      this.subscriptions.push(this.entityTabsComponent.entityTabsChanged.subscribe(
        (entityTabs) => {
          if (entityTabs) {
            if (this.viewInited) {
              this.matTabGroup._tabs.reset([...this.inclusiveTabs.toArray(), ...entityTabs]);
              this.matTabGroup._tabs.notifyOnChanges();
            } else {
              this.pendingTabs = entityTabs;
            }
          }
        }
      ));
    }
  }

  hideDetailsTabs(): boolean {
    return this.isEditValue && this.entitiesTableConfig.hideDetailsTabsOnEdit;
  }

  reload(): void {
    this.isEdit = false;
    this.entitiesTableConfig.loadEntity(this.currentEntityId).subscribe(
      (entity) => {
        this.entity = entity;
        this.entityComponent.entity = entity;
        if (this.entityTabsComponent) {
          this.entityTabsComponent.entity = entity;
        }
      }
    );
  }

  onCloseEntityDetails() {
    this.closeEntityDetails.emit();
  }

  onToggleEditMode(isEdit: boolean) {
    if (!isEdit) {
      this.entityComponent.entity = this.entity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.entity;
      }
      this.isEdit = isEdit;
    } else {
      this.isEdit = isEdit;
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

  helpLinkId(): string {
    if (this.resources.helpLinkIdForEntity && this.entityComponent.entityForm) {
      return this.resources.helpLinkIdForEntity(this.entityComponent.entityForm.getRawValue());
    } else {
      return this.resources.helpLinkId;
    }
  }

  saveEntity() {
    if (this.detailsForm.valid) {
      const editingEntity = mergeDeep(this.editingEntity, this.entityComponent.entityFormValue());
      this.entitiesTableConfig.saveEntity(editingEntity).subscribe(
        (entity) => {
          this.entity = entity;
          this.entityComponent.entity = entity;
          if (this.entityTabsComponent) {
            this.entityTabsComponent.entity = entity;
          }
          this.isEdit = false;
          this.entityUpdated.emit(this.entity);
        }
      );
    }
  }

  ngAfterViewInit(): void {
    this.viewInited = true;
    if (this.pendingTabs) {
      this.matTabGroup._tabs.reset([...this.inclusiveTabs.toArray(), ...this.pendingTabs]);
      this.matTabGroup._tabs.notifyOnChanges();
      this.pendingTabs = null;
    }
  }

}
