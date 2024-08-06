///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse,
  HttpStatusCode
} from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { EntityConflictDialogComponent } from '@shared/components/dialog/entity-conflict-dialog/entity-conflict-dialog.component';
import { EntityId } from '@shared/models/id/entity-id';

interface ConflictedEntity { version: number; id: EntityId }

@Injectable()
export class EntityConflictInterceptor implements HttpInterceptor {
  constructor(private dialog: MatDialog) {}

  intercept(request: HttpRequest<unknown & ConflictedEntity>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === HttpStatusCode.Conflict) {
          return this.resolveConflictRequest(request, error.error.message)
            .pipe(switchMap(httpRequest => next.handle(httpRequest)));
        } else {
          return throwError(() => error);
        }
      })
    );
  }

  private resolveConflictRequest(request: HttpRequest<unknown & ConflictedEntity>, message: string): Observable<HttpRequest<unknown>> {
    const dialogRef = this.dialog.open(EntityConflictDialogComponent, {data: {message, entityId: request.body.id}});

    return dialogRef.afterClosed().pipe(
      switchMap(result => {
        if (result) {
          request.body.version = null;
        }
        return of(request);
      })
    );
  }
}
