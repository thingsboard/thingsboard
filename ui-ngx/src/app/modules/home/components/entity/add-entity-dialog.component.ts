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

import { Component, ComponentFactoryResolver, Inject, Injector, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { EntityTypeResource, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { EntityComponent } from './entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-add-entity-dialog',
  templateUrl: './add-entity-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddEntityDialogComponent}],
  styleUrls: ['./add-entity-dialog.component.scss']
})
export class AddEntityDialogComponent extends
  DialogComponent<AddEntityDialogComponent, BaseData<HasId>> implements OnInit, ErrorStateMatcher {

  entityComponent: EntityComponent<BaseData<HasId>>;
  detailsForm: FormGroup;

  entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;
  translations: EntityTypeTranslation;
  resources: EntityTypeResource<BaseData<HasId>>;
  entity: BaseData<EntityId>;

  submitted = false;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityDialogData<BaseData<HasId>>,
              public dialogRef: MatDialogRef<AddEntityDialogComponent, BaseData<HasId>>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.entitiesTableConfig = this.data.entitiesTableConfig;
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.entity = {};
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
    const componentRef = viewContainerRef.createComponent(componentFactory, 0, injector);
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = true;
    this.detailsForm = this.entityComponent.entityForm;
  }

  helpLinkId(): string {
    if (this.resources.helpLinkIdForEntity && this.entityComponent.entityForm) {
      return this.resources.helpLinkIdForEntity(this.entityComponent.entityForm.getRawValue());
    } else {
      return this.resources.helpLinkId;
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    if (this.detailsForm.valid) {
      this.entity = {...this.entity, ...this.entityComponent.entityFormValue()};
      this.entitiesTableConfig.saveEntity(this.entity).subscribe(
        (entity) => {
          this.dialogRef.close(entity);
        }
      );
    }
  }
}
