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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import {
  ColorPickerDialogComponent,
  ColorPickerDialogData
} from '@shared/components/dialog/color-picker-dialog.component';
import {
  MaterialIconsDialogComponent,
  MaterialIconsDialogData
} from '@shared/components/dialog/material-icons-dialog.component';
import { ConfirmDialogComponent } from '@shared/components/dialog/confirm-dialog.component';
import { AlertDialogComponent } from '@shared/components/dialog/alert-dialog.component';
import { TodoDialogComponent } from '@shared/components/dialog/todo-dialog.component';

@Injectable(
  {
    providedIn: 'root'
  }
)
export class DialogService {

  constructor(
    private translate: TranslateService,
    private authService: AuthService,
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

  colorPicker(color: string): Observable<string> {
    return this.dialog.open<ColorPickerDialogComponent, ColorPickerDialogData, string>(ColorPickerDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          color
        }
    }).afterClosed();
  }

  materialIconPicker(icon: string): Observable<string> {
    return this.dialog.open<MaterialIconsDialogComponent, MaterialIconsDialogData, string>(MaterialIconsDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          icon
        }
      }).afterClosed();
  }

  private permissionDenied() {
    this.alert(
      this.translate.instant('access.permission-denied'),
      this.translate.instant('access.permission-denied-text'),
      this.translate.instant('action.close')
    );
  }

  forbidden(): Observable<boolean> {
    const observable = this.confirm(
      this.translate.instant('access.access-forbidden'),
      this.translate.instant('access.access-forbidden-text'),
      this.translate.instant('action.cancel'),
      this.translate.instant('action.sign-in'),
      true
    );
    observable.subscribe((res) => {
      if (res) {
        this.authService.logout();
      }
    });
    return observable;
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
