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

import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-widgets-bundle',
  templateUrl: './widgets-bundle.component.html',
  styleUrls: ['./widgets-bundle.component.scss']
})
export class WidgetsBundleComponent extends EntityComponent<WidgetsBundle> {

  @Input()
  standalone = false;

  constructor(protected store: Store<AppState>,
              @Optional() @Inject('entity') protected entityValue: WidgetsBundle,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<WidgetsBundle>,
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

  buildForm(entity: WidgetsBundle): UntypedFormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : ''],
        description: [entity  ? entity.description : '', Validators.maxLength(1024)],
        scada: [entity ? entity.scada : false],
        order: [entity ? entity.order : null]
      }
    );
  }

  updateForm(entity: WidgetsBundle) {
    this.entityForm.patchValue({
      title: entity.title,
      image: entity.image,
      description: entity.description,
      scada: entity.scada,
      order: entity.order
    });
  }
}
