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

import { Component, DestroyRef, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { isDefinedAndNotNull } from '@core/utils';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface ImportDialogData {
  importTitle: string;
  importFileLabel: string;
  enableImportFromContent?: boolean;
  importContentLabel?: string;
}

@Component({
    selector: 'tb-import-dialog',
    templateUrl: './import-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: ImportDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class ImportDialogComponent extends DialogComponent<ImportDialogComponent>
  implements OnInit, ErrorStateMatcher {

  importTitle: string;
  importFileLabel: string;
  enableImportFromContent: boolean;
  importContentLabel: string;

  importFormGroup: UntypedFormGroup;

  currentFileName: string;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ImportDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ImportDialogComponent>,
              private destroyRef: DestroyRef,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.importTitle = data.importTitle;
    this.importFileLabel = data.importFileLabel;
    this.enableImportFromContent = isDefinedAndNotNull(data.enableImportFromContent) ? data.enableImportFromContent : false;
    this.importContentLabel = data.importContentLabel;
  }

  ngOnInit(): void {
    this.importFormGroup = this.fb.group({
      importType: ['file'],
      fileContent: [null, [Validators.required]],
      jsonContent: [{ value: null, disabled: true }, [Validators.required]]
    });
    this.importFormGroup.get('importType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.importTypeChanged();
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  loadDataFromJsonContent(content: string): any {
    try {
      const importData = JSON.parse(content);
      return importData;
    } catch (err) {
      this.store.dispatch(new ActionNotificationShow({message: err.message, type: 'error'}));
      return null;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  importFromJson(): void {
    this.submitted = true;
    const importType: 'file' | 'content' = this.importFormGroup.get('importType').value;
    const importData = this.importFormGroup.get(importType === 'file' ? 'fileContent' : 'jsonContent').value;
    this.dialogRef.close(importData);
  }

  private importTypeChanged() {
    const importType: 'file' | 'content' = this.importFormGroup.get('importType').value;
    if (importType === 'file') {
      this.importFormGroup.get('fileContent').enable({emitEvent: false});
      this.importFormGroup.get('jsonContent').disable({emitEvent: false});
    } else {
      this.importFormGroup.get('fileContent').disable({emitEvent: false});
      this.importFormGroup.get('jsonContent').enable({emitEvent: false});
    }
  }
}
