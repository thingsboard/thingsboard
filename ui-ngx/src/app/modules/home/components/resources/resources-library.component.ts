// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectorRef, Component, Inject, Input, OnDestroy, OnInit, Optional } from '@angular/core';
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
import { startWith, takeUntil } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthState } from '@core/auth/auth.selectors';

@Component({
    selector: 'tb-resources-library',
    templateUrl: './resources-library.component.html',
    standalone: false
})
export class ResourcesLibraryComponent extends EntityComponent<Resource> implements OnInit, OnDestroy {

  @Input()
  standalone = false;

  @Input()
  resourceTypes = [ResourceType.LWM2M_MODEL, ResourceType.PKCS_12, ResourceType.JKS, ResourceType.GENERAL];

  @Input()
  defaultResourceType = ResourceType.LWM2M_MODEL;

  readonly resourceType = ResourceType;
  readonly resourceTypesTranslationMap = ResourceTypeTranslationMap;
  readonly maxResourceSize = getCurrentAuthState(this.store).maxResourceSize;

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: Resource,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Resource>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (this.isAdd) {
      this.observeResourceTypeChange();
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
      resourceType: [entity?.resourceType ? entity.resourceType : ResourceType.LWM2M_MODEL, Validators.required],
      fileName: [entity ? entity.fileName : null, Validators.required],
      data: [entity ? entity.data : null, this.isAdd ? [Validators.required] : []],
      descriptor: this.fb.group({
        mediaType: ['']
      })
    });
  }

  mediaTypeChange(mediaType: string): void {
    if (this.entityForm.get('resourceType').value === ResourceType.GENERAL) {
      this.entityForm.get('descriptor').get('mediaType').patchValue(mediaType);
    }
  }

  updateForm(entity: Resource): void {
    this.entityForm.patchValue(entity);
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('resourceType').disable({ emitEvent: false });
      this.entityForm.get('fileName').disable({ emitEvent: false });
      this.entityForm.get('data').disable({ emitEvent: false });
    }
    if (this.isAdd && this.resourceTypes.length === 1) {
      this.entityForm.get('resourceType').disable({ emitEvent: false });
    }
  }

  prepareFormValue(formValue: Resource): Resource {
    if (this.isEdit && !isDefinedAndNotNull(formValue.data)) {
      delete formValue.data;
    }
    return super.prepareFormValue(formValue);
  }

  getAllowedExtensions(): string {
    try {
      return ResourceTypeExtension.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '';
    }
  }

  getAcceptType(): string {
    try {
      return ResourceTypeMIMETypes.get(this.entityForm.get('resourceType').value);
    } catch (e) {
      return '*/*';
    }
  }

  convertToBase64File(data: string): string {
    return window.btoa(data);
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

  private observeResourceTypeChange(): void {
    this.entityForm.get('resourceType').valueChanges.pipe(
      startWith(this.defaultResourceType || ResourceType.LWM2M_MODEL),
      takeUntil(this.destroy$)
    ).subscribe((type: ResourceType) => this.onResourceTypeChange(type));
  }

  private onResourceTypeChange(type: ResourceType): void {
    if (type === this.resourceType.LWM2M_MODEL) {
      this.entityForm.get('title').disable({emitEvent: false});
      this.entityForm.patchValue({title: ''}, {emitEvent: false});
    } else {
      this.entityForm.get('title').enable({emitEvent: false});
    }
    this.entityForm.patchValue({
      data: null,
      fileName: null
    }, {emitEvent: false});
  }
}
