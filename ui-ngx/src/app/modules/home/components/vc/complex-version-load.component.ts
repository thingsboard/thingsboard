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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  createDefaultEntityTypesVersionLoad, EntityTypeLoadResult,
  EntityTypeVersionLoadRequest,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { TranslateService } from '@ngx-translate/core';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Observable, Subscription } from 'rxjs';
import { share } from 'rxjs/operators';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
  selector: 'tb-complex-version-load',
  templateUrl: './complex-version-load.component.html',
  styleUrls: ['./version-control.scss']
})
export class ComplexVersionLoadComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  onClose: (result: VersionLoadResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  loadVersionFormGroup: UntypedFormGroup;

  versionLoadResult: VersionLoadResult = null;

  entityTypeLoadResults: Array<EntityTypeLoadResult> = null;

  errorMessage: SafeHtml;

  hasError = false;

  versionLoadResult$: Observable<VersionLoadResult>;

  private versionLoadResultSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.loadVersionFormGroup = this.fb.group({
      entityTypes: [createDefaultEntityTypesVersionLoad(), []],
      rollbackOnError: [true]
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionLoadResultSubscription) {
      this.versionLoadResultSubscription.unsubscribe();
    }
  }

  entityTypeLoadResultMessage(result: EntityTypeLoadResult): string {
    const entityType = result.entityType;
    let message = this.translate.instant(entityTypeTranslations.get(entityType).typePlural) + ': ';
    const resultMessages: string[] = [];
    if (result.created) {
      resultMessages.push(this.translate.instant('version-control.created', {created: result.created}));
    }
    if (result.updated) {
      resultMessages.push(this.translate.instant('version-control.updated', {updated: result.updated}));
    }
    if (result.deleted) {
      resultMessages.push(this.translate.instant('version-control.deleted', {deleted: result.deleted}));
    }
    message += resultMessages.join(', ') + '.';
    return message;
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(this.versionLoadResult);
    }
  }

  restore(): void {
    const request: EntityTypeVersionLoadRequest = {
      versionId: this.versionId,
      entityTypes: this.loadVersionFormGroup.get('entityTypes').value,
      rollbackOnError: this.loadVersionFormGroup.get('rollbackOnError').value,
      type: VersionLoadRequestType.ENTITY_TYPE
    };
    this.versionLoadResult$ = this.entitiesVersionControlService.loadEntitiesVersion(request, {ignoreErrors: true}).pipe(
      share()
    );
    this.cd.detectChanges();
    if (this.popoverComponent) {
      this.popoverComponent.updatePosition();
    }
    this.versionLoadResultSubscription = this.versionLoadResult$.subscribe((result) => {
      this.versionLoadResult = result;
      this.entityTypeLoadResults = (result.result || []).filter(res => res.created || res.updated || res.deleted);
      if (result.error) {
        this.errorMessage = this.entitiesVersionControlService.entityLoadErrorToMessage(result.error);
      }
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    },
    (error) => {
      this.hasError = true;
      this.errorMessage = this.sanitizer.bypassSecurityTrustHtml(parseHttpErrorMessage(error, this.translate).message);
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }
}
