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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  entityTypesWithoutRelatedData,
  SingleEntityVersionCreateRequest,
  typesWithCalculatedFields,
  VersionCreateRequestType,
  VersionCreationResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of, Subscription } from 'rxjs';
import { EntityType } from '@shared/models/entity-type.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { share } from 'rxjs/operators';
import { parseHttpErrorMessage } from '@core/utils';

@Component({
  selector: 'tb-entity-version-create',
  templateUrl: './entity-version-create.component.html',
  styleUrls: ['./version-control.scss']
})
export class EntityVersionCreateComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  branch: string;

  @Input()
  entityId: EntityId;

  @Input()
  entityName: string;

  @Input()
  onClose: (result: VersionCreationResult | null, branch: string | null) => void;

  @Input()
  onBeforeCreateVersion: () => Observable<any>;

  @Input()
  popoverComponent: TbPopoverComponent;

  createVersionFormGroup: UntypedFormGroup;

  entityTypes = EntityType;

  entityTypesWithoutRelatedData = entityTypesWithoutRelatedData;

  resultMessage: string;

  versionCreateResult$: Observable<VersionCreationResult>;

  private versionCreateResultSubscription: Subscription;

  readonly typesWithCalculatedFields = typesWithCalculatedFields;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.createVersionFormGroup = this.fb.group({
      branch: [this.branch, [Validators.required]],
      versionName: [this.translate.instant('version-control.default-create-entity-version-name',
        {entityName: this.entityName}), [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      saveRelations: [false, []],
      saveAttributes: [true, []],
      saveCredentials: [true, []],
      saveCalculatedFields: [true, []]
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.versionCreateResultSubscription) {
      this.versionCreateResultSubscription.unsubscribe();
    }
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null, null);
    }
  }

  export(): void {
    const before = this.onBeforeCreateVersion ? this.onBeforeCreateVersion() : of(null);
    before.subscribe(() => {
      const request: SingleEntityVersionCreateRequest = {
        entityId: this.entityId,
        branch: this.createVersionFormGroup.get('branch').value,
        versionName: this.createVersionFormGroup.get('versionName').value,
        config: {
          saveRelations: !entityTypesWithoutRelatedData.has(this.entityId.entityType)
            ? this.createVersionFormGroup.get('saveRelations').value : false,
          saveAttributes: !entityTypesWithoutRelatedData.has(this.entityId.entityType)
            ? this.createVersionFormGroup.get('saveAttributes').value : false,
          saveCredentials: this.entityId.entityType === EntityType.DEVICE ? this.createVersionFormGroup.get('saveCredentials').value : false,
          saveCalculatedFields: typesWithCalculatedFields.has(this.entityId.entityType) ? this.createVersionFormGroup.get('saveCalculatedFields').value : false,
        },
        type: VersionCreateRequestType.SINGLE_ENTITY
      };
      this.versionCreateResult$ = this.entitiesVersionControlService.saveEntitiesVersion(request, {ignoreErrors: true}).pipe(
        share()
      );
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }

      this.versionCreateResultSubscription = this.versionCreateResult$.subscribe((result) => {
        if (result.done) {
          if (!result.added && !result.modified || result.error) {
            this.resultMessage = result.error ? result.error : this.translate.instant('version-control.nothing-to-commit');
            this.cd.detectChanges();
            if (this.popoverComponent) {
              this.popoverComponent.updatePosition();
            }
          } else if (this.onClose) {
            this.onClose(result, request.branch);
          }
        }
      },
      (error) => {
        this.resultMessage = parseHttpErrorMessage(error, this.translate).message;
        this.cd.detectChanges();
        if (this.popoverComponent) {
          this.popoverComponent.updatePosition();
        }
      });
    });
  }
}
