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

import { AfterViewInit, Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { AssetProfile } from '@shared/models/asset.models';
import { AssetProfileComponent } from '@home/components/profile/asset-profile.component';
import { AssetProfileService } from '@core/http/asset-profile.service';

export interface AssetProfileDialogData {
  assetProfile: AssetProfile;
  isAdd: boolean;
}

@Component({
    selector: 'tb-asset-profile-dialog',
    templateUrl: './asset-profile-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AssetProfileDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class AssetProfileDialogComponent extends
  DialogComponent<AssetProfileDialogComponent, AssetProfile> implements ErrorStateMatcher, AfterViewInit {

  isAdd: boolean;
  assetProfile: AssetProfile;

  submitted = false;

  @ViewChild('assetProfileComponent', {static: true}) assetProfileComponent: AssetProfileComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AssetProfileDialogData,
              public dialogRef: MatDialogRef<AssetProfileDialogComponent, AssetProfile>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private assetProfileService: AssetProfileService) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.assetProfile = this.data.assetProfile;
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.assetProfileComponent.entityForm.markAsDirty();
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
    if (this.assetProfileComponent.entityForm.valid) {
      this.assetProfile = {...this.assetProfile, ...this.assetProfileComponent.entityFormValue()};
      this.assetProfileService.saveAssetProfile(this.assetProfile).subscribe(
        (assetProfile) => {
          this.dialogRef.close(assetProfile);
        }
      );
    }
  }
}
