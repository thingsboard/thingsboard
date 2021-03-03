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

import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { Widget } from '@shared/models/widget.models';
import { toWidgetInfo } from '@home/models/widget-component.models';
import { distinctUntilChanged, map, mergeMap, publishReplay, refCount, share } from 'rxjs/operators';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit, OnChanges {

  private search$ = new BehaviorSubject<string>('');

  @Input()
  widgetsBundle: WidgetsBundle;

  @Input()
  aliasController: IAliasController;

  @Input()
  set searchBundle(search: string) {
    this.search$.next(search);
  }

  @Output()
  widgetSelected: EventEmitter<Widget> = new EventEmitter<Widget>();

  @Output()
  widgetsBundleSelected: EventEmitter<WidgetsBundle> = new EventEmitter<WidgetsBundle>();

  widgets: Array<Widget> = [];

  widgetsBundles$: Observable<Array<WidgetsBundle>>;

  widgets$: Observable<Array<Widget>>;

  constructor(private widgetsService: WidgetService,
              private sanitizer: DomSanitizer) {
  }

  ngOnInit(): void {
    this.widgetsBundles$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      mergeMap(search => this.fetchWidgetBundle(search))
    );
    this.widgets$ = this.search$.asObservable().pipe(
      distinctUntilChanged(),
      mergeMap(search => this.fetchWidget(search))
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue && (change.currentValue || change.currentValue === null)) {
        if (propName === 'widgetsBundle') {
          this.loadLibrary();
        }
      }
    }
  }

  private loadLibrary() {
    this.widgets.length = 0;
    if (this.widgetsBundle !== null) {
      const bundleAlias = this.widgetsBundle.alias;
      const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
      this.widgetsService.getBundleWidgetTypes(bundleAlias,
        isSystem).subscribe(
        (types) => {
          types = types.sort((a, b) => b.createdTime - a.createdTime);
          let top = 0;
          types.forEach((type) => {
            const widgetTypeInfo = toWidgetInfo(type);
            const widget: Widget = {
              typeId: type.id,
              isSystemType: isSystem,
              bundleAlias,
              typeAlias: widgetTypeInfo.alias,
              type: widgetTypeInfo.type,
              title: widgetTypeInfo.widgetName,
              image: widgetTypeInfo.image,
              description: widgetTypeInfo.description,
              sizeX: widgetTypeInfo.sizeX,
              sizeY: widgetTypeInfo.sizeY,
              row: top,
              col: 0,
              config: JSON.parse(widgetTypeInfo.defaultConfig)
            };
            widget.config.title = widgetTypeInfo.widgetName;
            this.widgets.push(widget);
            top += widget.sizeY;
          });
        }
      );
    }
  }

  hasWidgetTypes(): boolean {
    return this.widgets.length > 0;
  }

  onWidgetClicked($event: Event, widget: Widget): void {
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

  private fetchWidget(search: string): Observable<Array<Widget>> {
    return of(this.widgets).pipe(
      map(widgets => search ? widgets.filter(
        widget => (
          widget.title?.toLowerCase().includes(search.toLowerCase()) ||
          widget.description?.toLowerCase().includes(search.toLowerCase())
        )) : widgets
      )
    );
  }
}
