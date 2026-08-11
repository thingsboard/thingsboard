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
   * Emits null when nothing matches locally (older platform version, widget deleted by a sysadmin).
   *
   * There is no API to test a batch of fqns at once — `GET /api/widgetType?fqn=` is per fqn and
   * carries the full descriptor, and `/api/widgetTypeFqns` is scoped to a bundle the Hub payload
   * does not name — so this must stay a per-item check triggered by a click, never a prefetch
   * across a catalogue page.
   */
  resolveLocalWidgetType(item: MpItemVersionView): Observable<WidgetType | null> {
    const fqn = item?.dataDescriptor?.fqn;
    if (!isNotEmptyStr(fqn)) {
      return of(null);
    }
    // Hub items carry a bare fqn; platform widget types are addressed by a scoped full fqn.
    // Built-in content is system scoped, the tenant scope covers a previously installed copy.
    const fullFqns = isValidWidgetFullFqn(fqn) ? [fqn] : [`system.${fqn}`, `tenant.${fqn}`];
    return from(fullFqns).pipe(
      concatMap(fullFqn => this.widgetService.getWidgetType(fullFqn, { ignoreErrors: true, ignoreLoading: true }).pipe(
        catchError(() => of(null))
      )),
      first(widgetType => !!widgetType?.id?.id, null)
    );
  }

  /** Same match as `resolveLocalWidgetType`, resolved to the info projection widget pickers need. */
  resolveLocalWidgetTypeInfo(item: MpItemVersionView): Observable<WidgetTypeInfo | null> {
    return this.resolveLocalWidgetType(item).pipe(
      switchMap(widgetType => widgetType
        ? this.widgetService.getWidgetTypeInfoById(widgetType.id.id, { ignoreErrors: true, ignoreLoading: true }).pipe(
            catchError(() => of(null))
          )
        : of(null))
    );
  }

  /**
   * Navigates to the local component a built-in item mirrors. Emits false when there is nothing
   * to open — the platform version predates the component, or a sysadmin deleted it — leaving the
   * caller to decide what to offer instead. The check is deliberately lazy: see the note above
   * `resolveLocalWidgetType`, there is no bulk fqn lookup to prefetch a whole catalogue page with.
   */
  openLocalComponent(item: MpItemVersionView): Observable<boolean> {
    return this.resolveLocalComponentUrl(item).pipe(
      map(url => {
        if (!url) {
          return false;
        }
        void this.router.navigateByUrl(url);
        return true;
      })
    );
  }

  private resolveLocalComponentUrl(item: MpItemVersionView): Observable<string | null> {
    // Only widgets (including SCADA symbol widgets) ship inside the platform today. Other item
    // types have no local counterpart to match by fqn, so they fall through to the message above.
    if (item?.type !== ItemType.WIDGET) {
      return of(null);
    }
    return this.resolveLocalWidgetType(item).pipe(
      map(widgetType => widgetType
        ? getEntityDetailsPageURL(widgetType.id.id, EntityType.WIDGET_TYPE) || null
        : null)
    );
  }
}
