// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, DestroyRef, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RequestData } from '@shared/models/rpc.models';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-persistent-add-dialog',
    templateUrl: './persistent-add-dialog.component.html',
    styleUrls: ['./persistent-add-dialog.component.scss'],
    standalone: false
})

export class PersistentAddDialogComponent extends DialogComponent<PersistentAddDialogComponent, RequestData> implements OnInit {

  public persistentFormGroup: UntypedFormGroup;
  public rpcMessageTypeText: string;

  private requestData: RequestData = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<PersistentAddDialogComponent, RequestData>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);

    this.persistentFormGroup = this.fb.group(
      {
        method: ['', [Validators.required, Validators.pattern(/^\S+$/)]],
        oneWayElseTwoWay: [false],
        retries: [null, [Validators.pattern(/^-?[0-9]+$/), Validators.min(0)]],
        params: [null],
        additionalInfo: [null]
      }
    );
  }

  save() {
    this.requestData = this.persistentFormGroup.value;
    this.close();
  }

  ngOnInit(): void {
    this.rpcMessageTypeText = this.translate.instant('widgets.persistent-table.message-types.false');
    this.persistentFormGroup.get('oneWayElseTwoWay').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        this.rpcMessageTypeText = this.translate.instant(`widgets.persistent-table.message-types.${this.persistentFormGroup.get('oneWayElseTwoWay').value}`);
      }
    );
  }

  close(): void {
    this.dialogRef.close(this.requestData);
  }
}
