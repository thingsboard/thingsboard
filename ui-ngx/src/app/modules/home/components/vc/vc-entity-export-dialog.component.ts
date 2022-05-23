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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { EntityId } from '@shared/models/id/entity-id';
import {
  SingleEntityVersionCreateRequest,
  VersionCreateRequestType,
  VersionCreationResult
} from '@shared/models/vc.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';

export interface VcEntityExportDialogData {
  entityId: EntityId;
}

@Component({
  selector: 'tb-vc-entity-export-dialog',
  templateUrl: './vc-entity-export-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: VcEntityExportDialogComponent}],
  styleUrls: []
})
export class VcEntityExportDialogComponent extends DialogComponent<VcEntityExportDialogComponent>
  implements OnInit, ErrorStateMatcher {

  exportFormGroup: FormGroup;

  submitted = false;

  createResult: VersionCreationResult;

  createResultMessage: SafeHtml;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: VcEntityExportDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<VcEntityExportDialogComponent>,
              private entitiesVersionControlService: EntitiesVersionControlService,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.exportFormGroup = this.fb.group({
      branch: [null, [Validators.required]],
      versionName: [null, [Validators.required]],
      saveRelations: [false, []]
    });
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close();
  }

  export(): void {
    this.submitted = true;
    const request: SingleEntityVersionCreateRequest = {
      entityId: this.data.entityId,
      branch: this.exportFormGroup.get('branch').value,
      versionName: this.exportFormGroup.get('versionName').value,
      config: {
        saveRelations: this.exportFormGroup.get('saveRelations').value
      },
      type: VersionCreateRequestType.SINGLE_ENTITY
    };
    this.entitiesVersionControlService.saveEntitiesVersion(request).subscribe((result) => {
      this.createResult = result;
      const message = this.translate.instant('version-control.export-entity-version-result-message',
        {name: result.version.name, commitId: result.version.id});
      this.createResultMessage = this.domSanitizer.bypassSecurityTrustHtml(message);
    });
  }
}
