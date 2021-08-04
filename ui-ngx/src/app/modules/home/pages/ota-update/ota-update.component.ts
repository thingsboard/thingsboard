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
import { combineLatest, Subject } from 'rxjs';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EntityComponent } from '@home/components/entity/entity.component';
import {
  ChecksumAlgorithm,
  ChecksumAlgorithmTranslationMap,
  OtaPackage,
  OtaUpdateType,
  OtaUpdateTypeTranslationMap
} from '@shared/models/ota-package.models';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { filter, startWith, takeUntil } from 'rxjs/operators';
import { isNotEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-ota-update',
  templateUrl: './ota-update.component.html'
})
export class OtaUpdateComponent extends EntityComponent<OtaPackage> implements OnInit, OnDestroy {

  private destroy$ = new Subject();

  checksumAlgorithms = Object.values(ChecksumAlgorithm);
  checksumAlgorithmTranslationMap = ChecksumAlgorithmTranslationMap;
  packageTypes = Object.values(OtaUpdateType);
  otaUpdateTypeTranslationMap = OtaUpdateTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: OtaPackage,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<OtaPackage>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    super.ngOnInit();
    if (this.isAdd) {
      this.entityForm.get('isURL').valueChanges.pipe(
        takeUntil(this.destroy$)
      ).subscribe((isURL) => {
        if (isURL === false) {
          this.entityForm.get('url').clearValidators();
          this.entityForm.get('file').setValidators(Validators.required);
          this.entityForm.get('url').updateValueAndValidity({emitEvent: false});
          this.entityForm.get('file').updateValueAndValidity({emitEvent: false});
        } else {
          this.entityForm.get('file').clearValidators();
          this.entityForm.get('url').setValidators([Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]);
          this.entityForm.get('file').updateValueAndValidity({emitEvent: false});
          this.entityForm.get('url').updateValueAndValidity({emitEvent: false});
        }
      });
      combineLatest([
        this.entityForm.get('title').valueChanges.pipe(startWith('')),
        this.entityForm.get('version').valueChanges.pipe(startWith(''))
      ]).pipe(
        filter(() => this.entityForm.get('tag').pristine),
        takeUntil(this.destroy$)
      ).subscribe(([title, version]) => {
        const tag = (`${title} ${version}`).trim();
        this.entityForm.get('tag').patchValue(tag);
      });
    }
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

  buildForm(entity: OtaPackage): FormGroup {
    const form = this.fb.group({
      title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
      version: [entity ? entity.version : '', [Validators.required, Validators.maxLength(255)]],
      tag: [entity ? entity.tag : '', [Validators.maxLength(255)]],
      type: [entity?.type ? entity.type : OtaUpdateType.FIRMWARE, Validators.required],
      deviceProfileId: [entity ? entity.deviceProfileId : null, Validators.required],
      checksumAlgorithm: [entity && entity.checksumAlgorithm ? entity.checksumAlgorithm : ChecksumAlgorithm.SHA256],
      checksum: [entity ? entity.checksum : '', Validators.maxLength(1020)],
      url: [entity ? entity.url : ''],
      isURL: [false],
      additionalInfo: this.fb.group(
        {
          description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
        }
      )
    });
    if (this.isAdd) {
      form.addControl('file', this.fb.control(null, Validators.required));
      form.addControl('generateChecksum', this.fb.control(true));
    } else {
      form.addControl('fileName', this.fb.control(null));
      form.addControl('dataSize', this.fb.control(null));
      form.addControl('contentType', this.fb.control(null));
    }
    return form;
  }

  updateForm(entity: OtaPackage) {
    this.entityForm.patchValue({
      title: entity.title,
      version: entity.version,
      tag: entity.tag,
      type: entity.type,
      deviceProfileId: entity.deviceProfileId,
      checksumAlgorithm: entity.checksumAlgorithm,
      checksum: entity.checksum,
      fileName: entity.fileName,
      dataSize: entity.dataSize,
      contentType: entity.contentType,
      url: entity.url,
      isURL: isNotEmptyStr(entity.url),
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
    if (!this.isAdd && this.entityForm.enabled) {
      this.entityForm.disable({emitEvent: false});
      this.entityForm.get('additionalInfo').enable({emitEvent: false});
    }
  }

  onPackageIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('ota-update.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onPackageChecksumCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('ota-update.checksum-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onPackageDirectUrlCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('ota-update.checksum-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  prepareFormValue(formValue: any): any {
    if (formValue.isURL) {
      delete formValue.file;
    } else {
      delete formValue.url;
    }
    delete formValue.generateChecksum;
    return super.prepareFormValue(formValue);
  }
}
