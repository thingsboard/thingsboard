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

import { Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetsBundleComponent } from '@home/pages/widget/widgets-bundle.component';
import { WidgetService } from '@core/http/widget.service';

export interface WidgetsBundleDialogData {
  widgetsBundle: WidgetsBundle;
}

@Component({
    selector: 'tb-widgets-bundle-dialog',
    templateUrl: './widgets-bundle-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: WidgetsBundleDialogComponent }],
    styleUrls: ['widgets-bundle-dialog.component.scss'],
    standalone: false
})
export class WidgetsBundleDialogComponent extends
  DialogComponent<WidgetsBundleDialogComponent, WidgetsBundle> implements ErrorStateMatcher {

  widgetsBundle: WidgetsBundle;

  submitted = false;

  @ViewChild('widgetsBundleComponent', {static: true}) widgetsBundleComponent: WidgetsBundleComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: WidgetsBundleDialogData,
              public dialogRef: MatDialogRef<WidgetsBundleDialogComponent, WidgetsBundle>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private widgetsService: WidgetService) {
    super(store, router, dialogRef);
    this.widgetsBundle = this.data.widgetsBundle;
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
    if (this.widgetsBundleComponent.entityForm.valid) {
      this.widgetsBundle = {...this.widgetsBundle, ...this.widgetsBundleComponent.entityFormValue()};
      this.widgetsService.saveWidgetsBundle(this.widgetsBundle).subscribe((widgetBundle) => {
        this.dialogRef.close(widgetBundle);
      });
    }
  }

}
