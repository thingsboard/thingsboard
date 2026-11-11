// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormGroupDirective,
  NgForm,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ShareGroupRequest } from '@shared/models/entity-group.models';
import { EntityGroupId } from '@shared/models/id/entity-group-id';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface ShareEntityGroupDialogData {
  entityGroupId: EntityGroupId;
  isWriteAllowed?: boolean;
}

@Component({
    selector: 'tb-share-entity-group-dialog',
    templateUrl: './share-entity-group-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: ShareEntityGroupDialogComponent }],
    standalone: false
})
export class ShareEntityGroupDialogComponent extends
  DialogComponent<ShareEntityGroupDialogComponent, boolean> implements OnInit, ErrorStateMatcher {

  shareEntityGroupFormGroup: UntypedFormGroup;

  entityGroupId = this.data.entityGroupId;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ShareEntityGroupDialogData,
              public dialogRef: MatDialogRef<ShareEntityGroupDialogComponent, boolean>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityGroupService: EntityGroupService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);

    const shareGroupRequest: ShareGroupRequest = {
      ownerId: null,
      allUserGroup: true,
      readElseWrite: true
    };

    this.shareEntityGroupFormGroup = this.fb.group({
      shareGroupRequest: [shareGroupRequest, Validators.required]
    });
  }

  ngOnInit(): void {

  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  share(): void {
    this.submitted = true;
    if (this.shareEntityGroupFormGroup.valid) {
      const shareGroupRequest = this.shareEntityGroupFormGroup.get('shareGroupRequest').value;
      this.entityGroupService.shareEntityGroupV2(this.entityGroupId.id, shareGroupRequest).subscribe(
        () => {
          this.dialogRef.close(true);
        }
      );
    }
  }
}
