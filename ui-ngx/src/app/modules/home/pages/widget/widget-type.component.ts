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
  styleUrls: []
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
