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

import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { from, Observable, of } from 'rxjs';
import { catchError, concatMap, first, map, switchMap } from 'rxjs/operators';
import { WidgetService } from '@core/http/widget.service';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { isValidWidgetFullFqn, WidgetType, WidgetTypeInfo } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL, isNotEmptyStr } from '@core/utils';

/**
 * Result of looking up the local component a built-in Hub item mirrors. `failed` separates
 * "the lookup itself did not answer" (network error, 5xx, no read permission) from "the component
 * is not here": only the latter may be offered as an install, otherwise a transient error would
 * hand the tenant a duplicate of a component that is still in place.
 */
export interface IotHubLocalLookup<T> {
  value: T | null;
  failed: boolean;
}

/**
 * Outcome of trying to open the local component of a built-in item. 'cancelled' means the component
 * is there but the router did not go — typically a route guard blocked it (unsaved changes) — so
 * there is nothing to install and nothing to report.
 */
export type IotHubOpenLocalOutcome = 'opened' | 'cancelled' | 'missing' | 'failed';

/**
 * Handles IoT Hub items marked `builtIn` — content that already ships inside ThingsBoard.
 * Such an item is never installed: its `fqn` is deliberately identical to the fqn of the
 * platform's own component, which is used as the match key to reach the local copy.
 */
@Injectable({
  providedIn: 'root'
})
export class IotHubBuiltInService {

  constructor(
    private router: Router,
    private widgetService: WidgetService
  ) {}

  /**
   * Resolves the platform's own widget type mirroring a built-in Hub item, matched by fqn.
   *
   * There is no API to test a batch of fqns at once — `GET /api/widgetType?fqn=` is per fqn and
   * carries the full descriptor, and `/api/widgetTypeFqns` is scoped to a bundle the Hub payload
   * does not name — so this must stay a per-item check triggered by a click, never a prefetch
   * across a catalogue page.
   */
  resolveLocalWidgetType(item: MpItemVersionView): Observable<IotHubLocalLookup<WidgetType>> {
    const noMatch: IotHubLocalLookup<WidgetType> = { value: null, failed: false };
    const fqn = item?.dataDescriptor?.fqn;
    if (!isNotEmptyStr(fqn)) {
      // Without a match key nothing was asked of the server, so existence is unknown rather than
      // disproven — reporting it as absent would offer an install for a component that is in place.
      return of({ value: null, failed: true });
    }
    // Hub items carry a bare fqn; platform widget types are addressed by a scoped full fqn.
    // Built-in content is system scoped; the tenant scope covers a copy installed into this tenant,
    // which matters while the installed-items list a caller filters by is still stale.
    const fullFqns = isValidWidgetFullFqn(fqn) ? [fqn] : [`system.${fqn}`, `tenant.${fqn}`];
    return from(fullFqns).pipe(
      concatMap(fullFqn => this.widgetService.getWidgetType(fullFqn, { ignoreErrors: true, ignoreLoading: true }).pipe(
        map(widgetType => this.lookupResult(widgetType)),
        catchError((err: HttpErrorResponse) => of(this.lookupFailure<WidgetType>(err)))
      )),
      // Stop at the first match, and at the first hard failure too: with existence unknown, the
      // remaining candidate cannot turn a failure into a trustworthy "not here".
      first(lookup => !!lookup.value?.id?.id || lookup.failed, noMatch)
    );
  }

  /** Same match, resolved to the info projection widget pickers need (image, description, type). */
  resolveLocalWidgetTypeInfo(item: MpItemVersionView): Observable<IotHubLocalLookup<WidgetTypeInfo>> {
    return this.resolveLocalWidgetType(item).pipe(
      switchMap(lookup => lookup.value
        ? this.widgetService.getWidgetTypeInfoById(lookup.value.id.id, { ignoreErrors: true, ignoreLoading: true }).pipe(
            map(widgetTypeInfo => this.lookupResult(widgetTypeInfo)),
            catchError((err: HttpErrorResponse) => of(this.lookupFailure<WidgetTypeInfo>(err)))
          )
        : of({ value: null, failed: lookup.failed }))
    );
  }

  /** Navigates to the local component a built-in item mirrors, reporting why it could not. */
  openLocalComponent(item: MpItemVersionView): Observable<IotHubOpenLocalOutcome> {
    return this.resolveLocalComponentUrl(item).pipe(
      switchMap(lookup => {
        if (!lookup.value) {
          return of<IotHubOpenLocalOutcome>(lookup.failed ? 'failed' : 'missing');
        }
        // The navigation itself can still be refused — a ConfirmOnExitGuard on the page the user is
        // leaving, a failing resolver — and reporting it as opened would leave them with the Hub
        // dialog closed (it closes on NavigationStart) and nothing opened in its place.
        return from(this.router.navigateByUrl(lookup.value)).pipe(
          map((navigated): IotHubOpenLocalOutcome => navigated ? 'opened' : 'cancelled'),
          catchError(() => of<IotHubOpenLocalOutcome>('cancelled'))
        );
      })
    );
  }

  private resolveLocalComponentUrl(item: MpItemVersionView): Observable<IotHubLocalLookup<string>> {
    // Only widgets (including SCADA symbol widgets) ship inside the platform today. Other item
    // types have no local counterpart to match by fqn, so they count as missing.
    if (item?.type !== ItemType.WIDGET) {
      return of({ value: null, failed: false });
    }
    return this.resolveLocalWidgetType(item).pipe(
      map(lookup => ({
        value: lookup.value ? getEntityDetailsPageURL(lookup.value.id.id, EntityType.WIDGET_TYPE) || null : null,
        failed: lookup.failed
      }))
    );
  }

  private lookupResult<T>(value: T | null): IotHubLocalLookup<T> {
    return { value: value ?? null, failed: false };
  }

  // A 404 is an answer — the component is not here. Anything else (network error, 5xx, 403) leaves
  // existence unknown, and must never be presented to the user as deleted content.
  private lookupFailure<T>(err: HttpErrorResponse): IotHubLocalLookup<T> {
    return { value: null, failed: err?.status !== 404 };
  }
}
