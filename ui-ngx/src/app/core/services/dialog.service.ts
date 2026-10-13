// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { ProgressDialogComponent, ProgressDialogData } from '@shared/components/dialog/progress-dialog.component';
import {
  SubscriptionEntry,
  subscriptionEntryToEntityType,
  SubscriptionErrorCode,
  SubscriptionErrorData,
  subscriptionErrorsMap
} from '@shared/models/subscription.models';
import {
  EntityLimitDialogComponent,
  EntityLimitDialogData
} from '@shared/components/dialog/entity-limit-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EntityLimitExceededDialogComponent,
  EntityLimitExceededDialogData
} from '@shared/components/dialog/entity-limit-exceeded-dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestWhiteLabelingDialogComponent } from '@shared/components/dialog/request-white-labeling-dialog.component';
import {
  RequestPackWhiteLabelingDialogComponent
} from '@app/shared/components/dialog/request-pack-white-labeling-dialog.component';

@Injectable({
  providedIn: 'root'
})
export class DialogService {

  constructor(
    private store: Store<AppState>,
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

  colorPicker(color: string, colorClearButton = false, useThemePalette = false, disableAlpha = false, defaultColor = '#fff'): Observable<ColorPickerDialogResult> {
    return this.dialog.open<ColorPickerDialogComponent, ColorPickerDialogData, ColorPickerDialogResult>(ColorPickerDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          color,
          defaultColor,
          colorClearButton,
          useThemePalette,
          disableAlpha
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

  entitiesLimitExceeded(entityLimitData: {entityType: EntityType, limit: number, subscriptionViolation: boolean}): Observable<any> {
    return this.dialog.open<EntityLimitExceededDialogComponent, EntityLimitExceededDialogData>(EntityLimitExceededDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: entityLimitData,
        autoFocus: false
      }).afterClosed();
  }

  subscriptionViolation(error: SubscriptionErrorData): Observable<any> {
    const subscriptionErrorCode = error.subscriptionErrorCode;
    const subscriptionEntry = error.subscriptionEntry;
    if (subscriptionErrorCode === SubscriptionErrorCode.LIMIT_REACHED) {
      return this.entityLimit(error);
    } else if (subscriptionErrorCode === SubscriptionErrorCode.FEATURE_DISABLED &&
      subscriptionEntry === SubscriptionEntry.WHITE_LABELING) {
      return this.whiteLabelingFeature();
    } else {
      return this.subscriptionAlert(error);
    }
  }

  entityLimit(error: SubscriptionErrorData): Observable<any> {
    const subscriptionErrorCode = error.subscriptionErrorCode;
    const subscriptionEntry = error.subscriptionEntry;
    const value = error.subscriptionValue;
    if (getCurrentAuthState(this.store).licenseVersion > 1) {
      const entityType = subscriptionEntryToEntityType.get(subscriptionEntry);
      const limit = value;
      return this.entitiesLimitExceeded({entityType, limit, subscriptionViolation: true});
    } else {
      return this.dialog.open<EntityLimitDialogComponent, EntityLimitDialogData>(EntityLimitDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            subscriptionErrorCode,
            subscriptionEntry,
            value
          }
        }).afterClosed();
    }
  }

  whiteLabelingFeature(): Observable<any> {
    const authState = getCurrentAuthState(this.store);
    if (authState.communityGrantLicense) {
      return this.dialog.open<RequestPackWhiteLabelingDialogComponent>(RequestPackWhiteLabelingDialogComponent,
        {
          disableClose: true,
          autoFocus: false,
          panelClass: ['tb-dialog', 'tb-wide-dialog'],
        }).afterClosed();
    } else {
      return this.dialog.open<RequestWhiteLabelingDialogComponent>(RequestWhiteLabelingDialogComponent,
        {
          disableClose: true,
          autoFocus: false,
          panelClass: ['tb-dialog', 'tb-wide-dialog'],
        }).afterClosed();
    }
  }

  subscriptionAlert(error: SubscriptionErrorData): Observable<any> {
    const subscriptionErrorCode = error.subscriptionErrorCode;
    const subscriptionEntry = error.subscriptionEntry;
    const value = error.subscriptionValue;
    let content: string;
    if (subscriptionEntry && value && subscriptionErrorsMap.has(subscriptionErrorCode) &&
        subscriptionErrorsMap.get(subscriptionErrorCode).has(subscriptionEntry)) {
      const subscriptionErrorText = subscriptionErrorsMap.get(subscriptionErrorCode).get(subscriptionEntry);
      content = this.translate.instant(subscriptionErrorText, {value: value.value});
    } else {
      content = error.message;
    }
    return this.alert(
      this.translate.instant('subscription-error.title'),
      content,
      this.translate.instant('action.ok'),
      true
    );
  }

  permissionDenied() {
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

  progress<T>(progressObservable: Observable<T>, progressText: string): Observable<T> {
    return this.dialog.open<ProgressDialogComponent<T>, ProgressDialogData<T>, T>(ProgressDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
        data: {
          progressObservable,
          progressText
        }
      }).afterClosed();
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
