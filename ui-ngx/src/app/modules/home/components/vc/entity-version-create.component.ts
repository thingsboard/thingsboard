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

import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  SingleEntityVersionCreateRequest,
  VersionCreateRequestType,
  VersionCreationResult
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { EntityId } from '@shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-entity-version-create',
  templateUrl: './entity-version-create.component.html',
  styleUrls: ['./entity-version-create.component.scss']
})
export class EntityVersionCreateComponent extends PageComponent implements OnInit {

  @Input()
  branch: string;

  @Input()
  entityId: EntityId;

  @Input()
  onClose: (result: VersionCreationResult | null, branch: string | null) => void;

  @Input()
  onContentUpdated: () => void;

  createVersionFormGroup: FormGroup;

  resultMessage: string;

  constructor(protected store: Store<AppState>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.createVersionFormGroup = this.fb.group({
      branch: [this.branch, [Validators.required]],
      versionName: [null, [Validators.required]],
      saveRelations: [false, []]
    });
  }

  cancel(): void {
    if (this.onClose) {
      this.onClose(null, null);
    }
  }

  export(): void {
    const request: SingleEntityVersionCreateRequest = {
      entityId: this.entityId,
      branch: this.createVersionFormGroup.get('branch').value,
      versionName: this.createVersionFormGroup.get('versionName').value,
      config: {
        saveRelations: this.createVersionFormGroup.get('saveRelations').value
      },
      type: VersionCreateRequestType.SINGLE_ENTITY
    };
    this.entitiesVersionControlService.saveEntitiesVersion(request).subscribe((result) => {
      if (!result.added && !result.modified) {
        this.resultMessage = this.translate.instant('version-control.nothing-to-commit');
        if (this.onContentUpdated) {
          this.onContentUpdated();
        }
      } else if (this.onClose) {
        this.onClose(result, request.branch);
      }
    });
  }
}
