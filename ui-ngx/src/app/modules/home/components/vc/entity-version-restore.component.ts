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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, Sanitizer } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import {
  EntityDataInfo,
  SingleEntityVersionLoadRequest, VersionCreationResult,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { delay, share } from 'rxjs/operators';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable, Subscription } from 'rxjs';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
    selector: 'tb-entity-version-restore',
    templateUrl: './entity-version-restore.component.html',
    styleUrls: ['./version-control.scss'],
    standalone: false
})
export class EntityVersionRestoreComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  versionName: string;

  @Input()
  versionId: string;

  @Input()
  externalEntityId: EntityId;

  @Input()
  onClose: (result: VersionLoadResult | null) => void;

  @Input()
  popoverComponent: TbPopoverComponent;

  entityDataInfo: EntityDataInfo = null;

  restoreFormGroup: UntypedFormGroup;

  errorMessage: SafeHtml;

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
    this.restoreFormGroup = this.fb.group({
      loadAttributes: [true, []],
      loadRelations: [true, []],
      loadCredentials: [true, []],
      loadCalculatedFields: [true, []]
    });
    this.entitiesVersionControlService.getEntityDataInfo(this.externalEntityId, this.versionId).subscribe((data) => {
      this.entityDataInfo = data;
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionLoadResultSubscription) {
      this.versionLoadResultSubscription.unsubscribe();
    }
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  restore(): void {
    const request: SingleEntityVersionLoadRequest = {
      versionId: this.versionId,
      externalEntityId: this.externalEntityId,
      config: {
        loadRelations: this.entityDataInfo.hasRelations ? this.restoreFormGroup.get('loadRelations').value : false,
        loadAttributes: this.entityDataInfo.hasAttributes ? this.restoreFormGroup.get('loadAttributes').value : false,
        loadCredentials: this.entityDataInfo.hasCredentials ? this.restoreFormGroup.get('loadCredentials').value : false,
        loadCalculatedFields: this.entityDataInfo.hasCalculatedFields ? this.restoreFormGroup.get('loadCalculatedFields').value : false
      },
      type: VersionLoadRequestType.SINGLE_ENTITY
    };
    this.versionLoadResult$ = this.entitiesVersionControlService.loadEntitiesVersion(request, {ignoreErrors: true}).pipe(
      share()
    );
    this.cd.detectChanges();
    if (this.popoverComponent) {
      this.popoverComponent.updatePosition();
    }
    this.versionLoadResultSubscription = this.versionLoadResult$.subscribe((result) => {
      if (result.done) {
        if (result.error) {
          this.errorMessage = this.entitiesVersionControlService.entityLoadErrorToMessage(result.error);
          this.cd.detectChanges();
          if (this.popoverComponent) {
            this.popoverComponent.updatePosition();
          }
        } else {
          if (this.onClose) {
            this.onClose(result);
          }
        }
      }
    },
    (error) => {
      this.errorMessage = this.sanitizer.bypassSecurityTrustHtml(parseHttpErrorMessage(error, this.translate).message);
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }
}
