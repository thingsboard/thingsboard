///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Inject,
  Injector,
  SkipSelf,
  ViewChild
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl, FormGroupDirective, NgForm } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { TenantProfile } from '@shared/models/tenant.model';
import { TenantProfileComponent } from './tenant-profile.component';
import { TenantProfileService } from '@core/http/tenant-profile.service';

export interface TenantProfileDialogData {
  tenantProfile: TenantProfile;
  isAdd: boolean;
}

@Component({
  selector: 'tb-tenant-profile-dialog',
  templateUrl: './tenant-profile-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: TenantProfileDialogComponent}],
  styleUrls: []
})
export class TenantProfileDialogComponent extends
  DialogComponent<TenantProfileDialogComponent, TenantProfile> implements ErrorStateMatcher, AfterViewInit {

  isAdd: boolean;
  tenantProfile: TenantProfile;

  submitted = false;

  @ViewChild('tenantProfileComponent', {static: true}) tenantProfileComponent: TenantProfileComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: TenantProfileDialogData,
              public dialogRef: MatDialogRef<TenantProfileDialogComponent, TenantProfile>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private tenantProfileService: TenantProfileService) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.tenantProfile = this.data.tenantProfile;
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.tenantProfileComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.tenantProfileComponent.entityForm.valid) {
      this.tenantProfile = {...this.tenantProfile, ...this.tenantProfileComponent.entityFormValue()};
      this.tenantProfileService.saveTenantProfile(this.tenantProfile).subscribe(
        (tenantProfile) => {
          this.dialogRef.close(tenantProfile);
        }
      );
    }
  }

}
