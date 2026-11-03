// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';
import {
  ColorPickerDialogComponent,
  ColorPickerDialogData,
  ColorPickerDialogResult
} from '@shared/components/dialog/color-picker-dialog.component';
import {
  MaterialIconsDialogComponent,
  MaterialIconsDialogData,
  MaterialIconsDialogResult
} from '@shared/components/dialog/material-icons-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '@shared/components/dialog/confirm-dialog.component';
import { AlertDialogComponent, AlertDialogData } from '@shared/components/dialog/alert-dialog.component';
import {
  ErrorAlertDialogComponent,
  ErrorAlertDialogData
} from '@shared/components/dialog/error-alert-dialog.component';
import { TodoDialogComponent } from '@shared/components/dialog/todo-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class DialogService {

  constructor(
    private translate: TranslateService,
    private authService: AuthService,
    public dialog: MatDialog
  ) {
  }

  confirm(title: string, message: string, cancel: string = null, ok: string = null, fullscreen: boolean = false): Observable<boolean> {
    const dialogConfig: MatDialogConfig<ConfirmDialogData> = {
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
    const dialogRef = this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  alert(title: string, message: string, ok: string = null, fullscreen: boolean = false): Observable<boolean> {
    const dialogConfig: MatDialogConfig<AlertDialogData> = {
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
    const dialogRef = this.dialog.open<AlertDialogComponent, AlertDialogData, boolean>(AlertDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  errorAlert(title: string, message: string, error: any, ok: string = null, fullscreen: boolean = false): Observable<boolean> {
    const dialogConfig: MatDialogConfig<ErrorAlertDialogData> = {
      disableClose: true,
      data: {
        title,
        message,
        error,
        ok: ok || this.translate.instant('action.ok')
      }
    };
    if (fullscreen) {
      dialogConfig.panelClass = ['tb-fullscreen-dialog'];
    }
    const dialogRef = this.dialog.open<ErrorAlertDialogComponent, ErrorAlertDialogData, boolean>(ErrorAlertDialogComponent, dialogConfig);
    return dialogRef.afterClosed();
  }

  colorPicker(color: string, colorClearButton = false): Observable<ColorPickerDialogResult> {
    return this.dialog.open<ColorPickerDialogComponent, ColorPickerDialogData, ColorPickerDialogResult>(ColorPickerDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          color,
          colorClearButton
        },
        autoFocus: false
    }).afterClosed();
  }

  materialIconPicker(icon: string, iconClearButton = false): Observable<MaterialIconsDialogResult> {
    return this.dialog.open<MaterialIconsDialogComponent, MaterialIconsDialogData, MaterialIconsDialogResult>(MaterialIconsDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          icon,
          iconClearButton
        },
        autoFocus: false
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
