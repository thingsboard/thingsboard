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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { EntityId } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import {
  AttributeData,
  AttributeScope,
  isClientSideTelemetryType,
  SharedTelemetrySubscriber,
  TelemetryType
} from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NgZone } from '@angular/core';

export class AttributeDatasource implements DataSource<AttributeData> {

  private attributesSubject = new BehaviorSubject<AttributeData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<AttributeData>>(emptyPageData<AttributeData>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<AttributeData>(true, []);

  private allAttributes: Observable<Array<AttributeData>>;
  private telemetrySubscriber: SharedTelemetrySubscriber;

  constructor(private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<AttributeData[] | ReadonlyArray<AttributeData>> {
    return this.attributesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.attributesSubject.complete();
    this.pageDataSubject.complete();
    if (this.telemetrySubscriber) {
      this.telemetrySubscriber.unsubscribe();
      this.telemetrySubscriber = null;
    }
  }

  loadAttributes(entityId: EntityId, attributesScope: TelemetryType,
                 pageLink: PageLink, reload: boolean = false): Observable<PageData<AttributeData>> {
    if (reload) {
      this.allAttributes = null;
      if (this.telemetrySubscriber) {
        this.telemetrySubscriber.unsubscribe();
        this.telemetrySubscriber = null;
      }
    }
    this.selection.clear();
    const result = new ReplaySubject<PageData<AttributeData>>();
    this.fetchAttributes(entityId, attributesScope, pageLink).pipe(
      catchError(() => of(emptyPageData<AttributeData>())),
    ).subscribe(
      (pageData) => {
        this.attributesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchAttributes(entityId: EntityId, attributesScope: TelemetryType,
                  pageLink: PageLink): Observable<PageData<AttributeData>> {
    return this.getAllAttributes(entityId, attributesScope).pipe(
      map((data) => {
        const filteredData = data.filter(attrData => attrData.lastUpdateTs !== 0);
        return pageLink.filterData(filteredData);
      })
    );
  }

  getAllAttributes(entityId: EntityId, attributesScope: TelemetryType): Observable<Array<AttributeData>> {
    if (!this.allAttributes) {
      let attributesObservable: Observable<Array<AttributeData>>;
      if (isClientSideTelemetryType.get(attributesScope)) {
        this.telemetrySubscriber = SharedTelemetrySubscriber.createEntityAttributesSubscription(
          this.telemetryWsService, entityId, attributesScope, this.zone);
        this.telemetrySubscriber.subscribe();
        attributesObservable = this.telemetrySubscriber.attributeData$;
      } else {
        attributesObservable = this.attributeService.getEntityAttributes(entityId, attributesScope as AttributeScope);
      }
      this.allAttributes = attributesObservable.pipe(
        publishReplay(1),
        refCount()
      );
    }
    return this.allAttributes;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.attributesSubject.pipe(
      map((attributes) => numSelected === attributes.length)
    );
  }

  isEmpty(): Observable<boolean> {
    return this.attributesSubject.pipe(
      map((attributes) => !attributes.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.attributesSubject.pipe(
      tap((attributes) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === attributes.length) {
          this.selection.clear();
        } else {
          attributes.forEach(row => {
            this.selection.select(row);
          });
        }
      }),
      take(1)
    ).subscribe();
  }

}
