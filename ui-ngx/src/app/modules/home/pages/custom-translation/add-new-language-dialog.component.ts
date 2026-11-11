// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, Validators } from '@angular/forms';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface AddNewLanguageDialogData {
  langs: string[];
}

@Component({
    selector: 'tb-add-new-language-dialog',
    templateUrl: './add-new-language-dialog.component.html',
    styleUrls: ['./add-new-language-dialog.component.scss'],
    standalone: false
})
export class AddNewLanguageDialogComponent extends
  DialogComponent<AddNewLanguageDialogComponent> implements OnInit, OnDestroy{

  languageForm = this.fb.group({
    language: ['', {nonNullable: true, validators: Validators.required}],
    upload: [false],
    translation: [{value: null, disabled: true}, {nonNullable: true, validators: Validators.required}]
  });

  langs: string[];

  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<AddNewLanguageDialogComponent>,
              @Inject(MAT_DIALOG_DATA) private data: AddNewLanguageDialogData,
              private fb: FormBuilder,
              private customTranslationService: CustomTranslationService) {
    super(store, router, dialogRef);
    this.langs = this.data.langs;
  }


  ngOnInit() {
    this.languageForm.get('upload').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      if (value) {
        this.languageForm.get('translation').enable({emitEvent: false});
      } else {
        this.languageForm.get('translation').disable({emitEvent: false});
      }
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  addLanguage($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const formValue = this.languageForm.value;
    this.customTranslationService.saveCustomTranslation(
      formValue.language, formValue.upload ? formValue.translation : {}
    ).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

}
