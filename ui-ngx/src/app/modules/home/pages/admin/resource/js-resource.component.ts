///
/// Copyright © 2016-2026 The Thingsboard Authors
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
  ResourceSubType,
  ResourceSubTypeTranslationMap,
  ResourceType,
  ResourceTypeExtension,
  ResourceTypeMIMETypes
} from '@shared/models/resource.models';
import { startWith, takeUntil } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { base64toString, isDefinedAndNotNull, stringToBase64 } from '@core/utils';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-js-resource',
  templateUrl: './js-resource.component.html'
})
export class JsResourceComponent extends EntityComponent<Resource> implements OnInit, OnDestroy {

  readonly ResourceSubType = ResourceSubType;
  readonly jsResourceSubTypes: ResourceSubType[] = [ResourceSubType.EXTENSION, ResourceSubType.MODULE];
  readonly ResourceSubTypeTranslationMap = ResourceSubTypeTranslationMap;
  readonly maxResourceSize = getCurrentAuthState(this.store).maxResourceSize;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Resource,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Resource>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (this.isAdd) {
      this.observeResourceSubTypeChange();
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  hideDelete(): boolean {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Resource): FormGroup {
    return this.fb.group({
      title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
      resourceSubType: [entity?.resourceSubType ? entity.resourceSubType : ResourceSubType.EXTENSION, Validators.required],
      fileName: [entity ? entity.fileName : null],
      data: [entity ? entity.data : null, this.isAdd ? [Validators.required] : []],
      content: [entity?.data?.length ? base64toString(entity.data) : '', Validators.required]
    });
  }

  updateForm(entity: Resource): void {
    this.entityForm.patchValue(entity);
    const content = entity.resourceSubType === ResourceSubType.MODULE && entity?.data?.length ? base64toString(entity.data) : '';
    this.entityForm.get('content').patchValue(content);
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('resourceSubType').disable({ emitEvent: false });
      this.updateResourceSubTypeFieldsState(this.entityForm.get('resourceSubType').value);
    }
  }

  prepareFormValue(formValue: Resource): Resource {
    if (this.isEdit && !isDefinedAndNotNull(formValue.data)) {
      delete formValue.data;
    }
    if (formValue.resourceSubType === ResourceSubType.MODULE) {
      if (!formValue.fileName) {
        formValue.fileName = formValue.title + '.js';
      }
      formValue.data = new File([(formValue as any).content], formValue.fileName, {
        type: 'text/javascript'
      });
      delete (formValue as any).content;
    }
    return super.prepareFormValue(formValue);
  }

  getAllowedExtensions(): string {
    return ResourceTypeExtension.get(ResourceType.JS_MODULE);
  }

  getAcceptType(): string {
    return ResourceTypeMIMETypes.get(ResourceType.JS_MODULE);
  }

  convertToBase64File(data: string): string {
    return stringToBase64(data);
  }

  onResourceIdCopied(): void {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('resource.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  uploadContentFromFile(content: string) {
    this.entityForm.get('content').patchValue(content);
    this.entityForm.markAsDirty();
  }

  private observeResourceSubTypeChange(): void {
    this.entityForm.get('resourceSubType').valueChanges.pipe(
      startWith(ResourceSubType.EXTENSION),
      takeUntil(this.destroy$)
    ).subscribe((subType: ResourceSubType) => this.onResourceSubTypeChange(subType));
  }

  private onResourceSubTypeChange(subType: ResourceSubType): void {
    this.updateResourceSubTypeFieldsState(subType);
    this.entityForm.patchValue({
      data: null,
      fileName: null
    }, {emitEvent: false});
  }

  private updateResourceSubTypeFieldsState(subType: ResourceSubType) {
    if (subType === ResourceSubType.EXTENSION) {
      this.entityForm.get('data').enable({ emitEvent: false });
      this.entityForm.get('fileName').enable({ emitEvent: false });
      this.entityForm.get('content').disable({ emitEvent: false });
    } else {
      this.entityForm.get('data').disable({ emitEvent: false });
      this.entityForm.get('fileName').disable({ emitEvent: false });
      this.entityForm.get('content').enable({ emitEvent: false });
    }
  }
}
