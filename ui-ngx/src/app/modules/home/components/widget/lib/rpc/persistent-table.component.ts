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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Injector,
  Input, NgZone, OnDestroy,
  OnInit,
  StaticProvider,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext } from '@home/models/widget-component.models';
import { WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { BehaviorSubject, merge, Observable, of, ReplaySubject, Subject, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import {
  constructTableCssString,
  isValidPageStepCount,
  isValidPageStepIncrement,
  noDataMessage,
  TableCellButtonActionDescriptor,
  TableWidgetSettings
} from '@home/components/widget/lib/table-widget.models';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { hashCode, isDefined, isDefinedAndNotNull, parseHttpErrorMessage } from '@core/utils';
import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import {
  PersistentRpc,
  PersistentRpcData,
  RequestData,
  RpcStatus,
  rpcStatusColors,
  rpcStatusTranslation
} from '@shared/models/rpc.models';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder, sortOrderFromString } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DialogService } from '@core/services/dialog.service';
import { DeviceService } from '@core/http/device.service';
import { MatDialog } from '@angular/material/dialog';
import {
  PersistentDetailsDialogComponent,
  PersistentDetailsDialogData
} from '@home/components/widget/lib/rpc/persistent-details-dialog.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import {
  PERSISTENT_FILTER_PANEL_DATA,
  PersistentFilterPanelComponent,
  PersistentFilterPanelData
} from '@home/components/widget/lib/rpc/persistent-filter-panel.component';
import { PersistentAddDialogComponent } from '@home/components/widget/lib/rpc/persistent-add-dialog.component';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { HttpErrorResponse } from '@angular/common/http';

interface PersistentTableWidgetSettings extends TableWidgetSettings {
  defaultSortOrder: string;
  defaultPageSize: number;
  displayPagination: boolean;
  enableStickyAction: boolean;
  enableStickyHeader: boolean;
  enableFilter: boolean;
  displayColumns: string[];
  displayDetails: boolean;
  allowDelete: boolean;
  allowSendRequest: boolean;
}

interface PersistentTableWidgetActionDescriptor extends TableCellButtonActionDescriptor {
  details?: boolean;
  delete?: boolean;
}

@Component({
    selector: 'tb-persistent-table-widget',
    templateUrl: './persistent-table.component.html',
    styleUrls: ['./persistent-table.component.scss', '../table-widget.scss'],
    standalone: false
})

export class PersistentTableComponent extends PageComponent implements OnInit, OnDestroy, AfterViewInit {

  @Input()
  ctx: WidgetContext;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  private settings: PersistentTableWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private enableFilterAction = true;
  private allowSendRequest = true;
  private defaultPageSize;
  private defaultSortOrder = '-createdTime';
  private rpcStatusFilter: RpcStatus;
  private displayDetails = true;
  private allowDelete = true;
  private displayTableColumns: string[];
  private widgetResize$: ResizeObserver;

  public persistentDatasource: PersistentDatasource;
  public noDataDisplayMessageText: string;
  public rpcStatusColor = rpcStatusColors;
  public rpcStatusTranslation = rpcStatusTranslation;
  public displayPagination = true;
  public enableStickyHeader = true;
  public enableStickyAction = true;
  public pageLink: PageLink;
  public pageSizeOptions = [];
  public actionCellButtonAction: PersistentTableWidgetActionDescriptor[] = [];
  public displayedColumns: string[];
  public hidePageSize = false;
  hasData = false;

  constructor(protected store: Store<AppState>,
              private elementRef: ElementRef,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              private translate: TranslateService,
              private dialogService: DialogService,
              private deviceService: DeviceService,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private zone: NgZone) {
    super(store);
  }

  ngOnInit() {
    this.ctx.$scope.persistentTableWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.initializeConfig();
    this.hasData = this.ctx.defaultSubscription.hasResolvedData;
    this.ctx.updateWidgetParams();
    if (this.displayPagination) {
      this.widgetResize$ = new ResizeObserver(() => {
        this.zone.run(() => {
          const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
          if (showHidePageSize !== this.hidePageSize) {
            this.hidePageSize = showHidePageSize;
            this.cd.markForCheck();
          }
        });
      });
      this.widgetResize$.observe(this.elementRef.nativeElement);
    }
  }

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
  }

  ngAfterViewInit(): void {
    if (this.displayPagination) {
      this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);
    }
    ((this.displayPagination ? merge(this.sort.sortChange, this.paginator.page) : this.sort.sortChange) as Observable<any>)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();
    this.updateData();
  }

  private initializeConfig() {

    this.displayPagination = isDefined(this.settings.displayPagination) ? this.settings.displayPagination : true;
    this.enableStickyHeader = isDefined(this.settings.enableStickyHeader) ? this.settings.enableStickyHeader : true;
    this.displayTableColumns = isDefined(this.settings.displayColumns) ? this.settings.displayColumns : [];
    this.enableStickyAction = isDefined(this.settings.enableStickyAction) ? this.settings.enableStickyAction : true;
    this.enableFilterAction = isDefined(this.settings.enableFilter) ? this.settings.enableFilter : true;
    this.displayDetails = isDefined(this.settings.displayDetails) ? this.settings.displayDetails : true;
    this.allowDelete = isDefined(this.settings.allowDelete) ? this.settings.allowDelete : true;
    this.allowSendRequest = isDefined(this.settings.allowSendRequest) ? this.settings.allowSendRequest : true;

    this.noDataDisplayMessageText =
      noDataMessage(this.widgetConfig.noDataDisplayMessage, 'widgets.persistent-table.no-request-prompt', this.utils, this.translate);

    this.displayedColumns = [...this.displayTableColumns];

    const pageSize = this.settings.defaultPageSize;
    let pageStepIncrement = isValidPageStepIncrement(this.settings.pageStepIncrement) ? this.settings.pageStepIncrement : null;
    let pageStepCount = isValidPageStepCount(this.settings.pageStepCount) ? this.settings.pageStepCount : null;

    if (Number.isInteger(pageSize) && pageSize > 0) {
      this.defaultPageSize = pageSize;
    }

    if (!this.defaultPageSize) {
      this.defaultPageSize = pageStepIncrement ?? 10;
    }

    if (!isDefinedAndNotNull(pageStepIncrement) || !isDefinedAndNotNull(pageStepCount)) {
      pageStepIncrement = this.defaultPageSize;
      pageStepCount = 3;
    }

    for (let i = 1; i <= pageStepCount; i++) {
      this.pageSizeOptions.push(pageStepIncrement * i);
    }

    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this.defaultSortOrder = this.settings.defaultSortOrder;
    }
    const sortOrder: SortOrder = sortOrderFromString(this.defaultSortOrder);
    this.pageLink = new PageLink(this.defaultPageSize, 0, null, sortOrder);
    this.pageLink.pageSize = this.displayPagination ? this.defaultPageSize : 1024;


    this.ctx.widgetActions = [
      {
        name: 'widgets.persistent-table.add',
        show: this.allowSendRequest,
        icon: 'add',
        onAction: $event => this.addPersistentRpcRequest($event)
      },
      {
        name: 'widgets.persistent-table.refresh',
        show: true,
        icon: 'refresh',
        onAction: () => this.reloadPersistentRequests()
      },
      {
        name: 'widgets.persistent-table.filter',
        show: this.enableFilterAction,
        icon: 'filter_list',
        onAction: $event => this.editFilter($event)
      }
    ];

    if (this.settings.displayDetails) {
      this.actionCellButtonAction.push(
        {
          displayName: this.translate.instant('widgets.persistent-table.details'),
          icon: 'more_horiz',
          details: true
        } as PersistentTableWidgetActionDescriptor
      );
    }
    if (this.settings.allowDelete) {
      this.actionCellButtonAction.push(
        {
          displayName: this.translate.instant('widgets.persistent-table.delete'),
          icon: 'delete',
          delete: true
        } as PersistentTableWidgetActionDescriptor
      );
    }
    if (this.actionCellButtonAction.length) {
      this.displayedColumns.push('actions');
    }

    this.persistentDatasource = new PersistentDatasource(this.translate, this.subscription, this.ctx);

    const cssString = constructTableCssString(this.widgetConfig);
    const cssParser = new cssjs();
    cssParser.testMode = false;
    const namespace = 'persistent-table-' + hashCode(cssString);
    cssParser.cssPreviewNamespace = namespace;
    cssParser.createStyleElement(namespace, cssString);
    $(this.elementRef.nativeElement).addClass(namespace);
  }

  private updateData() {
    if (this.displayPagination) {
      this.pageLink.page = this.paginator.pageIndex;
      this.pageLink.pageSize = this.paginator.pageSize;
    } else {
      this.pageLink.page = 0;
    }
    if (this.settings.defaultSortOrder && this.settings.defaultSortOrder.length) {
      this.defaultSortOrder = this.utils.customTranslation(this.settings.defaultSortOrder, this.settings.defaultSortOrder);
    }
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.persistentDatasource.loadPersistent(this.pageLink, this.rpcStatusFilter);
    this.ctx.detectChanges();
  }

  public onDataUpdated() {
    this.ctx.detectChanges();
  }

  reloadPersistentRequests() {
    if (this.displayPagination) {
      this.paginator.pageIndex = 0;
    }
    this.updateData();
  }

  deleteRpcRequest($event: Event, persistentRpc: PersistentRpc) {
    if ($event) {
      $event.stopPropagation();
    }
    if (persistentRpc && persistentRpc.id && persistentRpc.id.id !== NULL_UUID) {
      this.dialogService.confirm(
        this.translate.instant('widgets.persistent-table.delete-request-title'),
        this.translate.instant('widgets.persistent-table.delete-request-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          this.deviceService.deletePersistedRpc(persistentRpc.id.id).subscribe(() => {
            this.reloadPersistentRequests();
          });
        }
      });
    }
  }

  openRequestDetails($event: Event, persistentRpc: PersistentRpc) {
    if ($event) {
      $event.stopPropagation();
    }
    if (persistentRpc && persistentRpc.id && persistentRpc.id.id !== NULL_UUID) {
      this.dialog.open<PersistentDetailsDialogComponent, PersistentDetailsDialogData, boolean>
      (PersistentDetailsDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            persistentRequest: persistentRpc,
            allowDelete: this.allowDelete
          }
        }).afterClosed().subscribe(
        (res) => {
          if (res) {
            this.reloadPersistentRequests();
          }
        }
      );
    }
  }

  addPersistentRpcRequest($event: Event){
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<PersistentAddDialogComponent, RequestData>
    (PersistentAddDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed().subscribe(
      (requestData) => {
        if (requestData) {
          this.sendRequests(requestData);
        }
      }
    );
  }

  private sendRequests(requestData: RequestData) {
    let commandPromise;
    if (requestData.oneWayElseTwoWay) {
      commandPromise = this.ctx.controlApi.sendOneWayCommand(
        requestData.method,
        requestData.params, null,
        true, null,
        requestData.retries,
        requestData.additionalInfo
      );
    } else {
      commandPromise = this.ctx.controlApi.sendTwoWayCommand(
        requestData.method,
        requestData.params,
        null,
        true, null,
        requestData.retries,
        requestData.additionalInfo
      );
    }
    commandPromise.subscribe(
      () => {
        this.reloadPersistentRequests();
      }
    );
  }

  public onActionButtonClick($event: Event, persistentRpc: PersistentRpc, actionDescriptor: PersistentTableWidgetActionDescriptor) {
    if (actionDescriptor.details) {
      this.openRequestDetails($event, persistentRpc);
    }
    if (actionDescriptor.delete) {
      this.deleteRpcRequest($event, persistentRpc);
    }
  }

  private editFilter($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'bottom',
      overlayX: 'end',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const providers: StaticProvider[] = [
      {
        provide: PERSISTENT_FILTER_PANEL_DATA,
        useValue: {
          rpcStatus: this.rpcStatusFilter
        } as PersistentFilterPanelData
      },
      {
        provide: OverlayRef,
        useValue: overlayRef
      }
    ];
    const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
    const componentRef = overlayRef.attach(new ComponentPortal(PersistentFilterPanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result) {
        const result = componentRef.instance.result;
        this.rpcStatusFilter = result.rpcStatus;
        this.reloadPersistentRequests();
      }
    });
    this.ctx.detectChanges();
  }

  protected readonly rpcStatusColorsMap = rpcStatusColors;
}

class PersistentDatasource implements DataSource<PersistentRpcData> {

  private persistentSubject = new BehaviorSubject<PersistentRpcData[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<PersistentRpcData>>(emptyPageData<PersistentRpcData>());

  private rpcErrorText: string;
  private executingSubjects: Array<Subject<any>>;
  private executingRpcRequest = false;

  public dataLoading = true;
  public pageData$ = this.pageDataSubject.asObservable();

  constructor(private translate: TranslateService,
              private subscription: IWidgetSubscription,
              private ctx: WidgetContext) {
  }

  connect(collectionViewer: CollectionViewer): Observable<PersistentRpcData[] | ReadonlyArray<PersistentRpcData>> {
    return this.persistentSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.persistentSubject.complete();
    this.pageDataSubject.complete();
  }

  reset() {
    const pageData = emptyPageData<PersistentRpcData>();
    this.persistentSubject.next(pageData.data);
    this.pageDataSubject.next(pageData);
  }

  loadPersistent(pageLink: PageLink, rpcStatusFilter: RpcStatus) {
    this.dataLoading = true;

    const result = new ReplaySubject<PageData<PersistentRpcData>>();
    this.fetchEntities(pageLink, rpcStatusFilter).pipe(
      catchError(() => of(emptyPageData<PersistentRpcData>())),
    ).subscribe(
      (pageData) => {
        this.persistentSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
        this.dataLoading = false;
      }
    );
    return result;
  }

  fetchEntities(pageLink: PageLink, rpcStatusFilter: RpcStatus): Observable<PageData<PersistentRpcData>> {
    if (!this.subscription.rpcEnabled) {
      return throwError(new Error('Rpc disabled!'));
    } else if (!this.subscription.targetDeviceId) {
      return throwError(new Error('Target device is not set!'));
    }
    const rpcSubject: Subject<any> = new Subject<any>();

    this.ctx.deviceService.getPersistedRpcRequests(this.subscription.targetDeviceId, pageLink, rpcStatusFilter).subscribe(
      (responseBody) => {
        rpcSubject.next(responseBody);
        rpcSubject.complete();
      },
      (rejection: HttpErrorResponse) => {
        this.rpcErrorText = null;
        this.executingSubjects = [];

        const index = this.executingSubjects.indexOf(rpcSubject);
        if (index >= 0) {
          this.executingSubjects.splice(index, 1);
        }
        this.executingRpcRequest = this.executingSubjects.length > 0;
        this.subscription.options.callbacks.rpcStateChanged(this.subscription);
        if (!this.executingRpcRequest || rejection.status === 504) {
          this.subscription.rpcRejection = rejection;
          if (rejection.status === 504) {
            this.subscription.rpcErrorText = 'Request timeout';
          } else {
            this.subscription.rpcErrorText =  'Error : ' + rejection.status + ' - ' + rejection.statusText;
            const error = parseHttpErrorMessage(rejection, this.translate);
            if (error) {
              this.subscription.rpcErrorText += '</br>';
              this.subscription.rpcErrorText += error.message || '';
            }
          }
          this.subscription.callbacks.onRpcFailed(this.subscription);
        }
        rpcSubject.error(rejection);
      }
    );
    return rpcSubject.asObservable();
  }

  isEmpty(): Observable<boolean> {
    return this.persistentSubject.pipe(
      map((requests) => !requests.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }
}
