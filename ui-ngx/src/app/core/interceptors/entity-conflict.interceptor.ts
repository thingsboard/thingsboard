// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpParams, HttpRequest, HttpStatusCode } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import {
  EntityConflictDialogComponent
} from '@shared/components/dialog/entity-conflict-dialog/entity-conflict-dialog.component';
import { EntityInfoData, VersionedEntity } from '@shared/models/entity.models';
import { getInterceptorConfig } from './interceptor.util';
import { isDefined } from '@core/utils';
import { InterceptorConfig } from '@core/interceptors/interceptor-config';
import { RuleChainMetaData } from '@shared/models/rule-chain.models';

@Injectable()
export class EntityConflictInterceptor implements HttpInterceptor {

  constructor(
    private dialog: MatDialog,
  ) {}

  intercept(request: HttpRequest<VersionedEntity>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!request.url.startsWith('/api/')) {
      return next.handle(request);
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== HttpStatusCode.Conflict) {
          return throwError(() => error);
        }

        return this.handleConflictError(request, next, error);
      })
    );
  }

  private handleConflictError(
    request: HttpRequest<VersionedEntity>,
    next: HttpHandler,
    error: HttpErrorResponse
  ): Observable<HttpEvent<unknown>> {
    if (getInterceptorConfig(request).ignoreVersionConflict || !this.isVersionedEntity(request.body)) {
      return throwError(() => error);
    }

    return this.openConflictDialog(request.body, error.error.message).pipe(
      switchMap(result => {
        if (isDefined(result)) {
          if (result) {
            return next.handle(this.updateRequestVersion(request));
          }
          (request.params as HttpParams & { interceptorConfig: InterceptorConfig }).interceptorConfig.ignoreErrors = true;
          return throwError(() => error);
        }
        return of(null);
      })
    );
  }

  private updateRequestVersion(request: HttpRequest<VersionedEntity>): HttpRequest<VersionedEntity> {
    const body = { ...request.body, version: null };
    return request.clone({ body });
  }

  private isVersionedEntity(entity: VersionedEntity): boolean {
    return !!((entity as EntityInfoData)?.id ?? (entity as RuleChainMetaData)?.ruleChainId)
  }

  private openConflictDialog(entity: VersionedEntity, message: string): Observable<boolean> {
    const dialogRef = this.dialog.open(EntityConflictDialogComponent, {
      disableClose: true,
      data: { message, entity },
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
    });

    return dialogRef.afterClosed();
  }
}
