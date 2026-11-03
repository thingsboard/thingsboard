// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, combineLatest, Observable, of, ReplaySubject, Subscription } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { defaultPageLinkSearchFunction, PageLink, PageLinkSearchFunction } from '@shared/models/page/page-link';
import { catchError, map, take, tap } from 'rxjs/operators';
import { EntityId } from '@app/shared/models/id/entity-id';
import { TranslateService } from '@ngx-translate/core';
import {
  AttributeData,
  AttributeScope,
  isClientSideTelemetryType,
  SharedTelemetrySubscriber,
  TelemetryType,
  telemetryTypeTranslationsShort
} from '@shared/models/telemetry/telemetry.models';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NgZone } from '@angular/core';
import { AttributeService } from '@core/http/attribute.service';

export interface ScopedAttributeData extends AttributeData {
  scope: TelemetryType;
  scopeName: string;
}

const isSameAttribute = (a: ScopedAttributeData, b: ScopedAttributeData): boolean =>
  a.scope === b.scope && a.key === b.key;

interface ScopeAttributesSource {
  attributeData$: Observable<Array<AttributeData>>;
  subscribe(): void;
  unsubscribe(): void;
}

// Server and shared attributes are fetched via REST, so they have to be re-fetched after every change
class RestScopeAttributesSource implements ScopeAttributesSource {

  private attributeDataSubject = new ReplaySubject<Array<AttributeData>>(1);
  private fetchSubscription: Subscription;

  public attributeData$ = this.attributeDataSubject.asObservable();

  constructor(private attributeService: AttributeService,
              private entityId: EntityId,
              private scope: AttributeScope) {}

  subscribe() {
    this.fetchSubscription?.unsubscribe();
    this.fetchSubscription = this.attributeService.getEntityAttributes(this.entityId, this.scope).pipe(
      catchError(() => of([] as Array<AttributeData>))
    ).subscribe(attributes => this.attributeDataSubject.next(attributes));
  }

  unsubscribe() {
    this.fetchSubscription?.unsubscribe();
    this.attributeDataSubject.complete();
  }
}

const attributeSearchFunction: PageLinkSearchFunction<ScopedAttributeData> =
  ({scope, scopeName, ...attribute}, textSearch) => defaultPageLinkSearchFunction()(attribute, textSearch);

export class AttributeDatasource implements DataSource<ScopedAttributeData> {

  private attributesSubject = new BehaviorSubject<ScopedAttributeData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<ScopedAttributeData>>(emptyPageData<ScopedAttributeData>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<ScopedAttributeData>(true, [], true, isSameAttribute);

  private entityId: EntityId;
  private scopeSources = new Map<TelemetryType, ScopeAttributesSource>();
  private scopeAttributes$: Observable<Array<ScopedAttributeData>>;
  private dataSubscription: Subscription;

  constructor(private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<ScopedAttributeData[] | ReadonlyArray<ScopedAttributeData>> {
    return this.attributesSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.attributesSubject.complete();
    this.pageDataSubject.complete();
    this.releaseSubscriptions();
  }

  loadAttributes(entityId: EntityId, scopes: TelemetryType[],
                 pageLink: PageLink, reload: boolean = false): void {
    if (reload) {
      this.updateScopeSubscriptions(entityId, scopes);
    }
    this.selection.clear();
    this.dataSubscription?.unsubscribe();
    this.dataSubscription = this.getAttributes().pipe(
      map((data) => {
        const filteredData = data.filter(attrData => attrData.lastUpdateTs !== 0);
        return pageLink.filterData(filteredData, attributeSearchFunction);
      }),
      catchError(() => of(emptyPageData<ScopedAttributeData>()))
    ).subscribe(
      (pageData) => {
        const removed = this.selection.selected.filter(selected =>
          !pageData.data.some(attribute => isSameAttribute(attribute, selected)));
        if (removed.length) {
          this.selection.deselect(...removed);
        }
        this.attributesSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
      }
    );
  }

  getAttributes(): Observable<Array<ScopedAttributeData>> {
    return this.scopeAttributes$ ?? of([]);
  }

  private updateScopeSubscriptions(entityId: EntityId, scopes: TelemetryType[]) {
    if (this.entityId?.id !== entityId?.id) {
      this.resetSubscriptions();
      this.entityId = entityId;
    }
    for (const [scope, source] of this.scopeSources) {
      if (!scopes.includes(scope)) {
        source.unsubscribe();
        this.scopeSources.delete(scope);
      }
    }
    this.refreshAttributes(scopes);
    const scopeStreams = scopes.map(scope => this.scopeAttributes(scope));
    this.scopeAttributes$ = scopeStreams.length === 1 ? scopeStreams[0] :
      combineLatest(scopeStreams).pipe(
        map(attributes => attributes.flat())
      );
  }

  private scopeAttributes(scope: TelemetryType): Observable<Array<ScopedAttributeData>> {
    let source = this.scopeSources.get(scope);
    if (!source) {
      source = isClientSideTelemetryType.get(scope) ?
        SharedTelemetrySubscriber.createEntityAttributesSubscription(this.telemetryWsService, this.entityId, scope, this.zone) :
        new RestScopeAttributesSource(this.attributeService, this.entityId, scope as AttributeScope);
      source.subscribe();
      this.scopeSources.set(scope, source);
    }
    const scopeName = this.translate.instant(telemetryTypeTranslationsShort.get(scope));
    let scopedAttributes = new Map<string, ScopedAttributeData>();
    return source.attributeData$.pipe(
      map(attributes => {
        const updatedAttributes = new Map<string, ScopedAttributeData>();
        attributes.forEach(attribute => {
          const scopedAttribute = Object.assign(scopedAttributes.get(attribute.key) ?? {scope, scopeName}, attribute);
          updatedAttributes.set(attribute.key, scopedAttribute);
        });
        scopedAttributes = updatedAttributes;
        return Array.from(updatedAttributes.values());
      })
    );
  }

  // Re-fetches REST loaded scopes, live updated scopes are left as is
  refreshAttributes(scopes?: TelemetryType[]) {
    this.scopeSources.forEach((source, scope) => {
      if (source instanceof RestScopeAttributesSource && (!scopes || scopes.includes(scope))) {
        source.subscribe();
      }
    });
  }

  releaseSubscriptions() {
    this.dataSubscription?.unsubscribe();
    this.dataSubscription = null;
    this.resetSubscriptions();
  }

  resetSubscriptions() {
    this.scopeSources.forEach(source => source.unsubscribe());
    this.scopeSources.clear();
    this.scopeAttributes$ = null;
  }

  isAllSelected(): Observable<boolean> {
    return this.attributesSubject.pipe(
      map((attributes) => {
        const numSelected = this.selection.selected.length;
        return attributes.length > 0 && numSelected === attributes.length
      })
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
