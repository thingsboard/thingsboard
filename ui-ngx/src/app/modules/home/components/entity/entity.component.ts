// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData, HasId } from '@shared/models/base-data';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { ChangeDetectorRef, Directive, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { deepTrim } from '@core/utils';

// @dynamic
@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class EntityComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent implements OnInit {

  entityForm: UntypedFormGroup;

  isEditValue: boolean;

  isDetailsPage = false;

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.setEntitiesTableConfig(entitiesTableConfig);
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
    this.cd.markForCheck();
    this.updateFormState();
  }

  get isEdit() {
    return this.isEditValue;
  }

  get isAdd(): boolean {
    return this.entityValue && (!this.entityValue.id || !this.entityValue.id.id);
  }

  @Input()
  set entity(entity: T) {
    this.entityValue = entity;
    if (this.entityForm) {
      this.entityForm.markAsPristine();
      this.updateForm(entity);
    }
  }

  get entity(): T {
    return this.entityValue;
  }

  @Output()
  entityAction = new EventEmitter<EntityAction<T>>();

  protected constructor(protected store: Store<AppState>,
                        protected fb: UntypedFormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: C,
                        protected cd: ChangeDetectorRef) {
    super(store);
    this.entityForm = this.buildForm(this.entityValue);
  }

  ngOnInit() {
  }

  onEntityAction($event: Event, action: string) {
    const entityAction = {event: $event, action, entity: this.entity} as EntityAction<T>;
    let handled = false;
    if (this.entitiesTableConfig) {
      handled = this.entitiesTableConfig.onEntityAction(entityAction);
    }
    if (!handled) {
      this.entityAction.emit(entityAction);
    }
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  entityFormValue() {
    const formValue = this.entityForm ? {...this.entityForm.getRawValue()} : {};
    return this.prepareFormValue(formValue);
  }

  prepareFormValue(formValue: any): any {
    return deepTrim(formValue);
  }

  protected setEntitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }

  abstract buildForm(entity: T): UntypedFormGroup;

  abstract updateForm(entity: T);

}
