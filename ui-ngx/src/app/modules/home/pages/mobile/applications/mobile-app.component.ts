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

import { ChangeDetectorRef, Component, Inject, Optional, Renderer2, ViewContainerRef } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { FormBuilder, FormGroup, UntypedFormControl, Validators } from '@angular/forms';
import { randomAlphanumeric } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { MobileApp, MobileAppStatus, mobileAppStatusTranslations } from '@shared/models/mobile-app.models';
import { PlatformType, platformTypeTranslations } from '@shared/models/oauth2.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { EditorPanelComponent } from '@home/pages/mobile/common/editor-panel.component';

@Component({
  selector: 'tb-mobile-app',
  templateUrl: './mobile-app.component.html',
  styleUrls: ['./mobile-app.component.scss']
})
export class MobileAppComponent extends EntityComponent<MobileApp> {

  entityType = EntityType;

  platformTypes = [PlatformType.ANDROID, PlatformType.IOS];

  MobileAppStatus = MobileAppStatus;
  PlatformType = PlatformType;

  mobileAppStatuses = Object.keys(MobileAppStatus) as MobileAppStatus[];

  platformTypeTranslations = platformTypeTranslations;
  mobileAppStatusTranslations = mobileAppStatusTranslations;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: MobileApp,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<MobileApp>,
              protected cd: ChangeDetectorRef,
              public fb: FormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: MobileApp): FormGroup {
    const form = this.fb.group({
      pkgName: [entity?.pkgName ?? '', [Validators.required, Validators.maxLength(255),
        Validators.pattern(/^[a-zA-Z][a-zA-Z\d_]*(?:\.[a-zA-Z][a-zA-Z\d_]*)+$/)]],
      title: [entity?.title ?? '', [Validators.maxLength(255)]],
      platformType: [entity?.platformType ?? PlatformType.ANDROID],
      appSecret: [entity?.appSecret ? entity.appSecret : btoa(randomAlphanumeric(64)), [Validators.required, this.base64Format]],
      status: [entity?.status ? entity.status : MobileAppStatus.DRAFT],
      versionInfo: this.fb.group({
        minVersion: [entity?.versionInfo?.minVersion ? entity.versionInfo.minVersion : '', Validators.pattern(/^\d+\.\d+\.\d+(-[a-zA-Z\d-.]+)?(\+[a-zA-Z\d-.]+)?$/)],
        minVersionReleaseNotes: [entity?.versionInfo?.minVersionReleaseNotes ? entity.versionInfo.minVersionReleaseNotes : ''],
        latestVersion: [entity?.versionInfo?.latestVersion ? entity.versionInfo.latestVersion : '', Validators.pattern(/^\d+\.\d+\.\d+(-[a-zA-Z\d-.]+)?(\+[a-zA-Z\d-.]+)?$/)],
        latestVersionReleaseNotes: [entity?.versionInfo?.latestVersionReleaseNotes ? entity.versionInfo.latestVersionReleaseNotes : ''],
      }),
      storeInfo: this.fb.group({
        storeLink: [entity?.storeInfo?.storeLink ? entity.storeInfo.storeLink : '',
          Validators.pattern(/^https?:\/\/play\.google\.com\/store\/apps\/details\?id=[a-zA-Z0-9._]+(?:&[a-zA-Z0-9._-]+=[a-zA-Z0-9._%-]*)*$/)],
        sha256CertFingerprints: [entity?.storeInfo?.sha256CertFingerprints ? entity.storeInfo.sha256CertFingerprints : '',
          Validators.pattern(/^[A-Fa-f0-9]{2}(:[A-Fa-f0-9]{2}){31}$/)],
        appId: [entity?.storeInfo?.appId ? entity.storeInfo.appId : '', Validators.pattern(/^[A-Z0-9]{10}\.[a-zA-Z0-9]+(\.[a-zA-Z0-9]+)*$/)],
      }),
    });

    form.get('platformType').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: PlatformType) => {
      if (value === PlatformType.ANDROID) {
        form.get('storeInfo.sha256CertFingerprints').enable({emitEvent: false});
        form.get('storeInfo.appId').disable({emitEvent: false});
        form.get('storeInfo.storeLink').setValidators(Validators.pattern(/^https?:\/\/play\.google\.com\/store\/apps\/details\?id=[a-zA-Z0-9._]+(?:&[a-zA-Z0-9._-]+=[a-zA-Z0-9._%-]*)*$/));
      } else if (value === PlatformType.IOS) {
        form.get('storeInfo.sha256CertFingerprints').disable({emitEvent: false});
        form.get('storeInfo.appId').enable({emitEvent: false});
        form.get('storeInfo.storeLink').setValidators(Validators.pattern(/^https?:\/\/apps\.apple\.com\/[a-z]{2}\/app\/[\w-]+\/id\d{7,10}(?:\?[^\s]*)?$/));
      }
      form.get('storeInfo.storeLink').setValue('', {emitEvent: false});
    });

    form.get('status').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe((value: MobileAppStatus) => {
      if (value === MobileAppStatus.PUBLISHED) {
        form.get('storeInfo.storeLink').addValidators(Validators.required);
        form.get('storeInfo.sha256CertFingerprints')
          .addValidators(Validators.required);
        form.get('storeInfo.appId').addValidators(Validators.required);
      } else {
        form.get('storeInfo.storeLink').clearValidators();
        form.get('storeInfo.sha256CertFingerprints').removeValidators(Validators.required);
        form.get('storeInfo.appId').removeValidators(Validators.required);
      }
      form.get('storeInfo.storeLink').updateValueAndValidity({emitEvent: false});
      form.get('storeInfo.sha256CertFingerprints').updateValueAndValidity({emitEvent: false});
      form.get('storeInfo.appId').updateValueAndValidity({emitEvent: false});
    });

    return form;
  }

  updateForm(entity: MobileApp) {
    this.entityForm.patchValue(entity, {emitEvent: false});
  }

  override updateFormState(): void {
    super.updateFormState();
    if (this.isEdit && this.entityForm && !this.isAdd) {
      this.entityForm.get('status').updateValueAndValidity({onlySelf: true});
      this.entityForm.get('platformType').updateValueAndValidity({onlySelf: true});
      this.entityForm.get('platformType').disable({emitEvent: false});
      if (this.entityForm.get('platformType').value === PlatformType.ANDROID) {
        this.entityForm.get('storeInfo.appId').disable({emitEvent: false});
      } else if (this.entityForm.get('platformType').value === PlatformType.IOS) {
        this.entityForm.get('storeInfo.sha256CertFingerprints').disable({emitEvent: false});
      }
    }
    if (this.entityForm && this.isAdd) {
      this.entityForm.get('storeInfo.appId').disable({emitEvent: false});
    }
  }

  override prepareFormValue(value: MobileApp): MobileApp {
    value.storeInfo = this.entityForm.get('storeInfo').value;
    return super.prepareFormValue(value);
  }

  generateAppSecret($event: Event) {
    $event.stopPropagation();
    this.entityForm.get('appSecret').setValue(btoa(randomAlphanumeric(64)));
    this.entityForm.get('appSecret').markAsDirty();
  }

  editReleaseNote($event: Event, matButton: MatButton, isLatest: boolean) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        disabled: !(this.isAdd || this.isEdit),
        title: isLatest ? 'mobile.latest-version-release-notes' : 'mobile.min-version-release-notes',
        content: isLatest
          ? this.entityForm.get('versionInfo.latestVersionReleaseNotes').value
          : this.entityForm.get('versionInfo.minVersionReleaseNotes').value
      };
      const releaseNotesPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        hostView: this.viewContainerRef,
        componentType: EditorPanelComponent,
        preferredPlacement: ['leftOnly', 'leftBottomOnly', 'leftTopOnly'],
        context: ctx,
        showCloseButton: false,
        popoverContentStyle: {padding: '16px 24px'},
        isModal: true
      });
      releaseNotesPanelPopover.tbComponentRef.instance.popover = releaseNotesPanelPopover;
      releaseNotesPanelPopover.tbComponentRef.instance.editorContentApplied.subscribe((releaseNotes) => {
        releaseNotesPanelPopover.hide();
        if (isLatest) {
          this.entityForm.get('versionInfo.latestVersionReleaseNotes').setValue(releaseNotes);
          this.entityForm.get('versionInfo.latestVersionReleaseNotes').markAsDirty();
        } else {
          this.entityForm.get('versionInfo.minVersionReleaseNotes').setValue(releaseNotes);
          this.entityForm.get('versionInfo.minVersionReleaseNotes').markAsDirty();
        }
      });
    }
  }

  private base64Format(control: UntypedFormControl): { [key: string]: boolean } | null {
    if (control.value === '') {
      return null;
    }
    try {
      const value = atob(control.value);
      if (value.length < 64) {
        return {minLength: true};
      }
      return null;
    } catch (e) {
      return {base64: true};
    }
  }
}
