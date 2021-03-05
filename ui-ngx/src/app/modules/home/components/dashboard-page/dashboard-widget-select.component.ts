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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { WidgetInfo, widgetType } from '@shared/models/widget.models';
import { toWidgetInfo } from '@home/models/widget-component.models';
import { distinctUntilChanged, map, publishReplay, refCount, switchMap, tap } from 'rxjs/operators';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit {

  private search$ = new BehaviorSubject<string>('');
  private filterWidgetTypes$ = new BehaviorSubject<Array<widgetType>>(null);
  private widgetsInfo: Observable<Array<WidgetInfo>>;
  private widgetsBundleValue: WidgetsBundle;
  private widgetsType = new Set<widgetType>();

  widgets$: Observable<Array<WidgetInfo>>;
  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  @Input()
  set widgetsBundle(widgetBundle: WidgetsBundle) {
    this.widgetsInfo = null;
    this.widgetsType.clear();
    this.widgetsTypes.emit(this.widgetsType);
    this.filterWidgetTypes$.next(null);
    this.widgetsBundleValue = widgetBundle;
  }

  get widgetsBundle(): WidgetsBundle {
    return this.widgetsBundleValue;
  }

  @Input()
  aliasController: IAliasController;

  @Input()
  set searchBundle(search: string) {
    this.search$.next(search);
  }

  @Input()
  set filterWidgetTypes(widgetTypes: Array<widgetType>) {
    this.filterWidgetTypes$.next(widgetTypes);
  }

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

  @Output()
  widgetsBundleSelected: EventEmitter<WidgetsBundle> = new EventEmitter<WidgetsBundle>();

  @Output()
  widgetsTypes: EventEmitter<Set<widgetType>> = new EventEmitter<Set<widgetType>>();

  constructor(private widgetsService: WidgetService,
              private sanitizer: DomSanitizer) {
  }

  ngOnInit(): void {
    this.widgetsBundles$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      switchMap(search => this.fetchWidgetBundle(search))
    );
    this.widgets$ = combineLatest([this.search$.asObservable(), this.filterWidgetTypes$.asObservable()]).pipe(
      distinctUntilChanged((oldValue, newValue) => JSON.stringify(oldValue) === JSON.stringify(newValue)),
      switchMap(search => this.fetchWidget(...search))
    );
  }

  private getWidgets(): Observable<Array<WidgetInfo>> {
    if (!this.widgetsInfo) {
      if (this.widgetsBundle !== null) {
        const bundleAlias = this.widgetsBundle.alias;
        const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
        this.widgetsInfo = this.widgetsService.getBundleWidgetTypeInfos(bundleAlias, isSystem).pipe(
          map(widgets => widgets.sort((a, b) => b.createdTime - a.createdTime)),
          map(widgets => widgets.map((widgetTypeInfo) => {
            this.widgetsType.add(widgetTypeInfo.widgetType);
            const widget: WidgetInfo = {
              isSystemType: isSystem,
              bundleAlias,
              typeAlias: widgetTypeInfo.alias,
              type: widgetTypeInfo.widgetType,
              title: widgetTypeInfo.name,
              image: widgetTypeInfo.image,
              description: widgetTypeInfo.description
            };
            return widget;
          })),
          tap(() => this.widgetsTypes.emit(this.widgetsType)),
          publishReplay(1),
          refCount()
        );
      } else {
        this.widgetsInfo = of([]);
      }
    }
    return this.widgetsInfo;
  }

  onWidgetClicked($event: Event, widget: WidgetInfo): void {
    this.widgetSelected.emit(widget);
  }

  isSystem(item: WidgetsBundle): boolean {
    return item && item.tenantId.id === NULL_UUID;
  }

  selectBundle($event: Event, bundle: WidgetsBundle) {
    $event.preventDefault();
    this.widgetsBundle = bundle;
    this.search$.next('');
    this.widgetsBundleSelected.emit(bundle);
  }

  getPreviewImage(imageUrl: string | null): SafeUrl | string {
    if (isDefinedAndNotNull(imageUrl)) {
      return this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    }
    return '/assets/widget-preview-empty.svg';
  }

  private getWidgetsBundle(): Observable<Array<WidgetsBundle>> {
    return this.widgetsService.getAllWidgetsBundles().pipe(
      publishReplay(1),
      refCount()
    );
  }

  private fetchWidgetBundle(search: string): Observable<Array<WidgetsBundle>> {
    return this.getWidgetsBundle().pipe(
      map(bundles => search ? bundles.filter(
        bundle => (
          bundle.title?.toLowerCase().includes(search.toLowerCase()) ||
          bundle.description?.toLowerCase().includes(search.toLowerCase())
        )) : bundles
      )
    );
  }

  private fetchWidget(search: string, filter: widgetType[]): Observable<Array<WidgetInfo>> {
    return this.getWidgets().pipe(
      map(widgets => filter ? widgets.filter((widget) => filter.includes(widget.type)) : widgets),
      map(widgets => search ? widgets.filter(
        widget => (
          widget.title?.toLowerCase().includes(search.toLowerCase()) ||
          widget.description?.toLowerCase().includes(search.toLowerCase())
        )) : widgets
      )
    );
  }
}
