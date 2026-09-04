// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { WidgetTypeDetails } from '@shared/models/widget.models';

@Component({
    selector: 'tb-widget-type',
    templateUrl: './widget-type.component.html',
    styleUrls: [],
    standalone: false
})
export class WidgetTypeComponent extends EntityComponent<WidgetTypeDetails> {

  constructor(protected store: Store<AppState>,
              @Inject('entity') protected entityValue: WidgetTypeDetails,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<WidgetTypeDetails>,
              public fb: UntypedFormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: WidgetTypeDetails): UntypedFormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : ''],
        description: [entity  ? entity.description : '', Validators.maxLength(1024)],
        tags: [entity ? entity.tags : []],
        scada: [entity ? entity.scada : false],
        deprecated: [entity ? entity.deprecated : false]
      }
    );
  }

  updateForm(entity: WidgetTypeDetails) {
    this.entityForm.patchValue({
      name: entity.name,
      image: entity.image,
      description: entity.description,
      tags: entity.tags,
      scada: entity.scada,
      deprecated: entity.deprecated
    });
  }
}
