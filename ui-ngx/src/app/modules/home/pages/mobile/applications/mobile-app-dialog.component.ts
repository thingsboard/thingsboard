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

import { AfterViewInit, Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { MobileApp } from '@shared/models/mobile-app.models';
import type { MobileAppComponent } from '@home/pages/mobile/applications/mobile-app.component';
import { PlatformType } from '@shared/models/oauth2.models';
import { MobileAppService } from '@core/http/mobile-app.service';

export interface MobileAppDialogData {
  platformType: PlatformType;
  name?: string
}

@Component({
  selector: 'tb-mobile-app-dialog',
  templateUrl: './mobile-app-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: MobileAppDialogComponent}],
  styleUrls: []
})
export class MobileAppDialogComponent extends DialogComponent<MobileAppDialogComponent, MobileApp> implements OnDestroy, AfterViewInit, ErrorStateMatcher {

  submitted = false;

  @ViewChild('mobileAppComponent', {static: true}) mobileAppComponent: MobileAppComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<MobileAppDialogComponent, MobileApp>,
              private mobileAppService: MobileAppService,
              @Inject(MAT_DIALOG_DATA) public data: MobileAppDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.mobileAppComponent.entityForm.markAsDirty();
      if (this.data.name) {
        this.mobileAppComponent.entityForm.get('title').patchValue(this.data.name, {emitEvent: false});
      }
      this.mobileAppComponent.entityForm.patchValue({platformType: this.data.platformType});
      this.mobileAppComponent.entityForm.get('platformType').disable({emitEvent: false});
      this.mobileAppComponent.isEdit = true;
    }, 0);
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    this.submitted = true;
    if (this.mobileAppComponent.entityForm.valid) {
      this.mobileAppService.saveMobileApp(this.mobileAppComponent.entityFormValue()).subscribe(
        app =>  this.dialogRef.close(app)
      )
    }
  }
}
