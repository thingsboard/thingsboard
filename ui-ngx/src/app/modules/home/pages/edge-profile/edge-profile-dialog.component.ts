// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormControl, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { EdgeProfile } from '@shared/models/edge-profile.models';
import { EdgeProfileComponent } from '@home/pages/edge-profile/edge-profile.component';
import { EdgeProfileService } from '@core/http/edge-profile.service';
import { of } from 'rxjs';
import { mergeMap, tap } from 'rxjs/operators';

export interface EdgeProfileDialogData {
  edgeProfile: EdgeProfile;
  isAdd: boolean;
}

@Component({
  selector: 'tb-edge-profile-dialog',
  templateUrl: './edge-profile-dialog.component.html',
  providers: [{ provide: ErrorStateMatcher, useExisting: EdgeProfileDialogComponent }],
  styleUrls: [],
  standalone: false
})
export class EdgeProfileDialogComponent extends
  DialogComponent<EdgeProfileDialogComponent, EdgeProfile> implements ErrorStateMatcher, AfterViewInit {

  isAdd: boolean;
  edgeProfile: EdgeProfile;

  submitted = false;

  rootRuleChainControl = new UntypedFormControl(null, Validators.required);

  @ViewChild('edgeProfileComponent', {static: true}) edgeProfileComponent: EdgeProfileComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EdgeProfileDialogData,
              public dialogRef: MatDialogRef<EdgeProfileDialogComponent, EdgeProfile>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private edgeProfileService: EdgeProfileService) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.edgeProfile = this.data.edgeProfile;
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.edgeProfileComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.edgeProfileComponent.entityForm.invalid || (this.isAdd && this.rootRuleChainControl.invalid)) {
      return;
    }
    this.edgeProfile = {...this.edgeProfile, ...this.edgeProfileComponent.entityFormValue()};
    const rootRuleChainId: string = this.isAdd ? this.rootRuleChainControl.value : null;
    this.edgeProfileService.saveEdgeProfile(this.edgeProfile).pipe(
      // held onto so that a failed root assignment can be retried as an update instead of a duplicate create
      tap(saved => this.edgeProfile = saved),
      // the root rule chain is a server side assignment, so it can only be set once the profile exists
      mergeMap(saved => rootRuleChainId ? this.edgeProfileService.setRootRuleChain(saved.id.id, rootRuleChainId) : of(saved))
    ).subscribe(
      (edgeProfile) => {
        this.dialogRef.close(edgeProfile);
      }
    );
  }
}
