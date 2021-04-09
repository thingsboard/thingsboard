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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import {
  Resource,
  ResourceType,
  ResourceTypeExtension,
  ResourceTypeMIMETypes,
  ResourceTypeTranslationMap
} from '@shared/models/resource.models';
import { distinctUntilChanged, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-resources-library',
  templateUrl: './resources-library.component.html'
})
export class ResourcesLibraryComponent extends EntityComponent<Resource> implements OnInit, OnDestroy {

  readonly resourceType = ResourceType;
  readonly resourceTypes = Object.values(this.resourceType);
  readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;

  private destroy$ = new Subject();

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Resource,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Resource>,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    super.ngOnInit();
    this.entityForm.get('resourceType').valueChanges.pipe(
      distinctUntilChanged((oldValue, newValue) => [oldValue, newValue].includes(this.resourceType.LWM2M_MODEL)),
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      if (type === this.resourceType.LWM2M_MODEL) {
        this.entityForm.get('title').clearValidators();
      } else {
        this.entityForm.get('title').setValidators(Validators.required);
      }
      this.entityForm.get('title').updateValueAndValidity({emitEvent: false});
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Resource): FormGroup {
    return this.fb.group(
      {
        resourceType: [{value: entity?.resourceType ? entity.resourceType : ResourceType.LWM2M_MODEL,
                        disabled: this.isEdit }, [Validators.required]],
        data: [entity ? entity.data : null, [Validators.required]],
        fileName: [entity ? entity.fileName : null, [Validators.required]],
        title: [entity ? entity.title : '', []]
      }
    );
  }

  updateForm(entity: Resource) {
    this.entityForm.patchValue({resourceType: entity.resourceType});
    if (this.isEdit) {
      this.entityForm.get('resourceType').disable({emitEvent: false});
    }
    this.entityForm.patchValue({
      data: entity.data,
      fileName: entity.fileName,
      title: entity.title
    });
  }

  getAllowedExtensions() {
    try {
      return ResourceTypeExtension.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '';
    }
  }

  getAcceptType() {
    try {
      return ResourceTypeMIMETypes.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '*/*';
    }
  }
}
