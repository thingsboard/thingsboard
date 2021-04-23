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
import { ChecksumAlgorithm, ChecksumAlgorithmTranslationMap, Firmware } from '@shared/models/firmware.models';
import { distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Component({
  selector: 'tb-firmware',
  templateUrl: './firmwares.component.html'
})
export class FirmwaresComponent extends EntityComponent<Firmware> implements OnInit, OnDestroy {

  private destroy$ = new Subject();

  checksumAlgorithms = Object.values(ChecksumAlgorithm);
  checksumAlgorithmTranslationMap = ChecksumAlgorithmTranslationMap;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Firmware,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Firmware>,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    super.ngOnInit();
    if (this.isAdd) {
      this.entityForm.get('checksumAlgorithm').valueChanges.pipe(
        map(algorithm => !!algorithm),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      ).subscribe(
        setAlgorithm => {
          if (setAlgorithm) {
            this.entityForm.get('checksum').setValidators([Validators.maxLength(1020), Validators.required]);
          } else {
            this.entityForm.get('checksum').clearValidators();
          }
          this.entityForm.get('checksum').updateValueAndValidity({emitEvent: false});
        }
      );
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

  buildForm(entity: Firmware): FormGroup {
    const form = this.fb.group({
      title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
      version: [entity ? entity.version : '', [Validators.required, Validators.maxLength(255)]],
      checksumAlgorithm: [entity ? entity.checksumAlgorithm : null],
      checksum: [entity ? entity.checksum : '', Validators.maxLength(1020)],
      additionalInfo: this.fb.group(
        {
          description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
        }
      )
    });
    if (this.isAdd) {
      form.addControl('file', this.fb.control(null, Validators.required));
    } else {
      form.addControl('fileName', this.fb.control(null));
      form.addControl('dataSize', this.fb.control(null));
      form.addControl('contentType', this.fb.control(null));
    }
    return form;
  }

  updateForm(entity: Firmware) {
    this.entityForm.patchValue({
      title: entity.title,
      version: entity.version,
      checksumAlgorithm: entity.checksumAlgorithm,
      checksum: entity.checksum,
      fileName: entity.fileName,
      dataSize: entity.dataSize,
      contentType: entity.contentType,
      additionalInfo: {
        description: entity.additionalInfo ? entity.additionalInfo.description : ''
      }
    });
  }

  onFirmwareIdCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('firmware.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  onFirmwareChecksumCopied() {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('firmware.checksum-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
