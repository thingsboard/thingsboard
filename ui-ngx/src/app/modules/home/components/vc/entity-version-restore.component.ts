///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  EntityDataInfo,
  SingleEntityVersionLoadRequest,
  VersionLoadRequestType,
  VersionLoadResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { delay } from 'rxjs/operators';
import { SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'tb-entity-version-restore',
  templateUrl: './entity-version-restore.component.html',
  styleUrls: ['./version-control.scss']
})
export class EntityVersionRestoreComponent extends PageComponent implements OnInit {

  @Input()
  branch: string;

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

  restoreFormGroup: FormGroup;

  errorMessage: SafeHtml;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.restoreFormGroup = this.fb.group({
      loadAttributes: [true, []],
      loadRelations: [true, []],
      loadCredentials: [true, []]
    });
    this.entitiesVersionControlService.getEntityDataInfo(this.externalEntityId, this.versionId).subscribe((data) => {
      this.entityDataInfo = data;
      this.cd.detectChanges();
      if (this.popoverComponent) {
        this.popoverComponent.updatePosition();
      }
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null);
    }
  }

  restore(): void {
    const request: SingleEntityVersionLoadRequest = {
      branch: this.branch,
      versionId: this.versionId,
      externalEntityId: this.externalEntityId,
      config: {
        loadRelations: this.entityDataInfo.hasRelations ? this.restoreFormGroup.get('loadRelations').value : false,
        loadAttributes: this.entityDataInfo.hasAttributes ? this.restoreFormGroup.get('loadAttributes').value : false,
        loadCredentials: this.entityDataInfo.hasCredentials ? this.restoreFormGroup.get('loadCredentials').value : false
      },
      type: VersionLoadRequestType.SINGLE_ENTITY
    };
    this.entitiesVersionControlService.loadEntitiesVersion(request).subscribe((result) => {
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
    });
  }
}
