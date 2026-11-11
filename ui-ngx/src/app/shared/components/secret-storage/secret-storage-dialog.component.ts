// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  parseSecret,
  SecretStorage,
  secretStorageCreateTitleTranslationMap,
  SecretStorageInfo,
  SecretStorageType
} from '@shared/models/secret-storage.models';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { deepTrim } from '@core/utils';

export interface SecretStorageData  {
  type: SecretStorageType;
  value: string;
  fileName?: string;
  hideType?: boolean;
  onlyCreateNew?: boolean;
}

@Component({
    selector: 'tb-secret-storage-dialog',
    templateUrl: './secret-storage-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class SecretStorageDialogComponent extends DialogComponent<SecretStorageDialogComponent, SecretStorage | string> implements OnInit {

  createNewLabel: string;

  createNew = true;

  onlyCreateNew = true;

  hideType = true;

  secretType = SecretStorageType.TEXT;
  SecretStorageType = SecretStorageType;

  fileName: string;

  secretForm = this.fb.group({
    type: [SecretStorageType.TEXT, []],
    name: ['', [Validators.required, Validators.pattern('^[^{};]+$'), Validators.maxLength(255), Validators.pattern(/.*\S.*/)]],
    description: ['', []],
    value: ['', [Validators.required]]
  });
  secret = new FormControl<string>(null);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SecretStorageData,
              public dialogRef: MatDialogRef<SecretStorageDialogComponent, SecretStorage | string>,
              private destroyRef: DestroyRef,
              private fb: FormBuilder,
              private secretStorageService: SecretStorageService) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.createNewLabel = secretStorageCreateTitleTranslationMap.get(this.data.type);
    this.secretForm.get('type').patchValue(this.data.type, {emitEvent: false});
    this.secretType = this.data.type;
    this.fileName = this.data.fileName;
    this.hideType = this.data.hideType;
    this.onlyCreateNew = this.data.onlyCreateNew;

    this.secretForm.get('type').valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.secretForm.get('value').patchValue('', {emitEvent: false}));

    const secret = parseSecret(this.data.value);
    if (secret) {
      this.createNew = false;
      this.secret.enable({emitEvent: false});
      this.secret.patchValue(secret, {emitEvent: false});
      this.secretForm.disable({emitEvent: false});
    } else {
      this.secret.disable({emitEvent: false});
      this.secretForm.enable({emitEvent: false});
      this.createNew = true;
      this.secretForm.get('value').patchValue(this.data.value);
    }
  }

  onChange(value: boolean) {
    if (value) {
      this.secretForm.enable({emitEvent: false});
      this.secret.disable({emitEvent: false})
    } else {
      this.secretForm.disable({emitEvent: false});
      this.secret.enable({emitEvent: false})
    }
  }

  get dialogTitle(): string {
    return  this.createNew ? 'secret-storage.dialog-title' : 'secret-storage.use-secret';
  }

  get addButtonLabel(): string {
    return this.createNew ? 'action.add' : 'secret-storage.action-use';
  }

  helpLinkId(): string {
    return 'secretStorage';
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private prepareOutputSecret(secret: string, type: SecretStorageType): string {
    return '${secret:'+secret+';type:'+type+'}';
  }

  add(): void {
    if (this.createNew) {
      this.secretStorageService.saveSecret({
        ...deepTrim(this.secretForm.value),
        value: this.secretForm.value.value
      } as SecretStorageInfo).subscribe(
        (secret) => {
          if (this.onlyCreateNew) {
            this.dialogRef.close(secret);
          } else {
            this.dialogRef.close(this.prepareOutputSecret(secret.name, this.data.type));
          }
        }
      );
    } else {
      if (this.onlyCreateNew) {
        this.dialogRef.close(null);
      } else {
        this.dialogRef.close(this.prepareOutputSecret(this.secret.value, this.data.type));
      }
    }
  }
}
