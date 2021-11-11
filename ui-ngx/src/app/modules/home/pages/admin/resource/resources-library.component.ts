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

import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
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
import {filter, pairwise, startWith, takeUntil} from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';

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
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
    this.entityForm.get('resourceType').valueChanges.pipe(
      startWith(ResourceType.LWM2M_MODEL),
      filter(() => this.isAdd),
      takeUntil(this.destroy$)
    ).subscribe((type) => {
      if (type === this.resourceType.LWM2M_MODEL) {
        this.entityForm.get('title').disable({emitEvent: false});
        this.entityForm.patchValue({title: ''}, {emitEvent: false});
      } else {
        this.entityForm.get('title').enable({emitEvent: false})
      }
      this.entityForm.patchValue({
        data: null,
        fileName: null
      }, {emitEvent: false});
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
    const form = this.fb.group(
      {
        title: [entity ? entity.title : "", [Validators.required, Validators.maxLength(255)]],
        resourceType: [entity?.resourceType ? entity.resourceType : ResourceType.LWM2M_MODEL, [Validators.required]],
        fileName: [entity ? entity.fileName : null, [Validators.required]],
      }
    );
    if (this.isAdd) {
      form.addControl('data', this.fb.control(null, Validators.required));
    }
    return form;
  }

  updateForm(entity: Resource) {
    if (this.isEdit) {
      this.entityForm.get('resourceType').disable({emitEvent: false});
      this.entityForm.get('fileName').disable({emitEvent: false});
    }
    this.entityForm.patchValue({
      resourceType: entity.resourceType,
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

  convertToBase64File(data: string): string {
    return window.btoa(data);
  }

  onResourceIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('resource.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
