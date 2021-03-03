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
import { WidgetInfo } from '@shared/models/widget.models';
import { toWidgetInfo } from '@home/models/widget-component.models';
import { distinctUntilChanged, map, publishReplay, refCount, switchMap } from 'rxjs/operators';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit {

  private search$ = new BehaviorSubject<string>('');
  private widgetsTypes: Observable<Array<WidgetInfo>>;
  private widgetsBundleValue: WidgetsBundle;

  widgets$: Observable<Array<WidgetInfo>>;
  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  @Input()
  set widgetsBundle(widgetBundle: WidgetsBundle) {
    this.widgetsTypes = null;
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

  @Output()
  widgetSelected: EventEmitter<WidgetInfo> = new EventEmitter<WidgetInfo>();

  @Output()
  widgetsBundleSelected: EventEmitter<WidgetsBundle> = new EventEmitter<WidgetsBundle>();

  constructor(private widgetsService: WidgetService,
              private sanitizer: DomSanitizer) {
  }

  ngOnInit(): void {
    this.widgetsBundles$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      switchMap(search => this.fetchWidgetBundle(search))
    );
    this.widgets$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      switchMap(search => this.fetchWidget(search))
    );
  }

  private getWidgets(): Observable<Array<WidgetInfo>> {
    if (!this.widgetsTypes) {
      if (this.widgetsBundle !== null) {
        const bundleAlias = this.widgetsBundle.alias;
        const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
        this.widgetsTypes = this.widgetsService.getBundleWidgetTypes(bundleAlias, isSystem).pipe(
          map(widgets => widgets.sort((a, b) => b.createdTime - a.createdTime)),
          map(widgets => widgets.map((type) => {
            const widgetTypeInfo = toWidgetInfo(type);
            const widget: WidgetInfo = {
              isSystemType: isSystem,
              bundleAlias,
              typeAlias: widgetTypeInfo.alias,
              type: widgetTypeInfo.type,
              title: widgetTypeInfo.widgetName,
              image: widgetTypeInfo.image,
              description: widgetTypeInfo.description
            };
            return widget;
          })),
          publishReplay(1),
          refCount()
        );
      } else {
        this.widgetsTypes = of([]);
      }
    }
    return this.widgetsTypes;
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

  private fetchWidget(search: string): Observable<Array<WidgetInfo>> {
    return this.getWidgets().pipe(
      map(widgets => search ? widgets.filter(
        widget => (
          widget.title?.toLowerCase().includes(search.toLowerCase()) ||
          widget.description?.toLowerCase().includes(search.toLowerCase())
        )) : widgets
      )
    );
  }
}
