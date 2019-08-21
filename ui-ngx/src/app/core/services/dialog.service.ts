///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MatDialog, MatDialogConfig } from '@angular/material';
import { ConfirmDialogComponent } from '@core/services/dialog/confirm-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { AlertDialogComponent } from '@core/services/dialog/alert-dialog.component';
import {TodoDialogComponent} from "@core/services/dialog/todo-dialog.component";

@Injectable(
  {
    providedIn: 'root'
  }
)
export class DialogService {

  constructor(
    private translate: TranslateService,
    public dialog: MatDialog
  ) {
  }

  confirm(title: string, message: string, cancel: string = null, ok: string = null, fullscreen: boolean = false): Observable<boolean> {
    const dialogConfig: MatDialogConfig = {
      disableClose: true,
      data: {
        title,
        message,
        cancel: cancel || this.translate.instant('action.cancel'),
        ok: ok || this.translate.instant('action.ok')
      }
    };
    if (fullscreen) {
      dialogConfig.panelClass = ['tb-fullscreen-dialog'];
    }
    const dialogRef = this.dialog.open(ConfirmDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  alert(title: string, message: string, ok: string = null, fullscreen: boolean = false): Observable<boolean> {
    const dialogConfig: MatDialogConfig = {
      disableClose: true,
      data: {
        title,
        message,
        ok: ok || this.translate.instant('action.ok')
      }
    };
    if (fullscreen) {
      dialogConfig.panelClass = ['tb-fullscreen-dialog'];
    }
    const dialogRef = this.dialog.open(AlertDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  todo(): Observable<any> {
    const dialogConfig: MatDialogConfig = {
      disableClose: true,
      panelClass: ['tb-fullscreen-dialog']
    };
    const dialogRef = this.dialog.open(TodoDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

}
