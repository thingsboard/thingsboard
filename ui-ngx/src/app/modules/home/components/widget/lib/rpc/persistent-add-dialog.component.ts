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
  styleUrls: ['./persistent-add-dialog.component.scss']
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
