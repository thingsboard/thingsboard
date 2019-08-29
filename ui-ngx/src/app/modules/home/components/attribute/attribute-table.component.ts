///
/// Copyright © 2016-2019 The Thingsboard Authors
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
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AttributeData,
  AttributeScope,
  isClientSideTelemetryType, LatestTelemetry,
  TelemetryType,
  telemetryTypeTranslations
} from '@shared/models/telemetry/telemetry.models';
import { AttributeDatasource } from '@home/models/datasource/attribute-datasource';
import { AttributeService } from '@app/core/http/attribute.service';
import { EntityType } from '@shared/models/entity-type.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { RelationDialogComponent, RelationDialogData } from '@home/components/relation/relation-dialog.component';
import {
  AddAttributeDialogComponent,
  AddAttributeDialogData
} from '@home/components/attribute/add-attribute-dialog.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TIMEWINDOW_PANEL_DATA, TimewindowPanelComponent } from '@shared/components/time/timewindow-panel.component';
import {
  EDIT_ATTRIBUTE_VALUE_PANEL_DATA,
  EditAttributeValuePanelComponent,
  EditAttributeValuePanelData
} from './edit-attribute-value-panel.component';
import { ComponentPortal, PortalInjector } from '@angular/cdk/portal';


@Component({
  selector: 'tb-attribute-table',
  templateUrl: './attribute-table.component.html',
  styleUrls: ['./attribute-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttributeTableComponent extends PageComponent implements AfterViewInit, OnInit {

  telemetryTypeTranslationsMap = telemetryTypeTranslations;
  isClientSideTelemetryTypeMap = isClientSideTelemetryType;

  latestTelemetryTypes = LatestTelemetry;

  mode: 'default' | 'widget' = 'default';

  attributeScopes: Array<string> = [];
  attributeScope: TelemetryType;

  displayedColumns = ['select', 'lastUpdateTs', 'key', 'value'];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: AttributeDatasource;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  attributeScopeSelectionReadonly = false;

  viewsInited = false;

  private disableAttributeScopeSelectionValue: boolean;
  get disableAttributeScopeSelection(): boolean {
    return this.disableAttributeScopeSelectionValue;
  }
  @Input()
  set disableAttributeScopeSelection(value: boolean) {
    this.disableAttributeScopeSelectionValue = coerceBooleanProperty(value);
  }

  @Input()
  defaultAttributeScope: TelemetryType;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        if (this.viewsInited) {
          this.updateData(true);
        }
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    if (this.entityIdValue !== entityId) {
      this.entityIdValue = entityId;
      this.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild('searchInput', {static: false}) searchInputField: ElementRef;

  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;

  constructor(protected store: Store<AppState>,
              private attributeService: AttributeService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private dialogService: DialogService) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'key', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new AttributeDatasource(this.attributeService, this.translate);
  }

  ngOnInit() {
  }

  attributeScopeChanged(attributeScope: TelemetryType) {
    this.attributeScope = attributeScope;
    this.mode = 'default';
    this.updateData(true);
  }

  ngAfterViewInit() {

    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.viewsInited = true;
    if (this.activeValue && this.entityIdValue) {
      this.updateData(true);
    }
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadAttributes(this.entityIdValue, this.attributeScope, this.pageLink, reload);
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  resetSortAndFilter(update: boolean = true) {
    const entityType = this.entityIdValue.entityType;
    if (entityType === EntityType.DEVICE || entityType === EntityType.ENTITY_VIEW) {
      this.attributeScopes = Object.keys(AttributeScope);
      this.attributeScopeSelectionReadonly = false;
    } else {
      this.attributeScopes = [AttributeScope.SERVER_SCOPE];
      this.attributeScopeSelectionReadonly = true;
    }
    this.mode = 'default';
    this.attributeScope = this.defaultAttributeScope;
    this.pageLink.textSearch = null;
    if (this.viewsInited) {
      this.paginator.pageIndex = 0;
      const sortable = this.sort.sortables.get('key');
      this.sort.active = sortable.id;
      this.sort.direction = 'asc';
      if (update) {
        this.updateData(true);
      }
    }
  }

  reloadAttributes() {
    this.updateData(true);
  }

  addAttribute($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddAttributeDialogComponent, AddAttributeDialogData, boolean>(AddAttributeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityId: this.entityIdValue,
        attributeScope: this.attributeScope as AttributeScope
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reloadAttributes();
        }
      }
    );
  }

  editAttribute($event: Event, attribute: AttributeData) {
    if ($event) {
      $event.stopPropagation();
    }
    const target = $event.target || $event.srcElement || $event.currentTarget;
    const config = new OverlayConfig();
    config.backdropClass = 'cdk-overlay-transparent-backdrop';
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'end',
      originY: 'center',
      overlayX: 'end',
      overlayY: 'center'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
      .withPositions([connectedPosition]);

    const overlayRef = this.overlay.create(config);
    overlayRef.backdropClick().subscribe(() => {
      overlayRef.dispose();
    });
    const injectionTokens = new WeakMap<any, any>([
      [EDIT_ATTRIBUTE_VALUE_PANEL_DATA, {
        attributeValue: attribute.value
      } as EditAttributeValuePanelData],
      [OverlayRef, overlayRef]
    ]);
    const injector = new PortalInjector(this.viewContainerRef.injector, injectionTokens);
    const componentRef = overlayRef.attach(new ComponentPortal(EditAttributeValuePanelComponent,
      this.viewContainerRef, injector));
    componentRef.onDestroy(() => {
      if (componentRef.instance.result !== null) {
        const attributeValue = componentRef.instance.result;
        const updatedAttribute = {...attribute};
        updatedAttribute.value = attributeValue;
        this.attributeService.saveEntityAttributes(this.entityIdValue,
          this.attributeScope as AttributeScope, [updatedAttribute]).subscribe(
          () => {
            this.reloadAttributes();
          }
        );
      }
    });
  }

  deleteAttributes($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.dataSource.selection.selected.length > 0) {
      this.dialogService.confirm(
        this.translate.instant('attribute.delete-attributes-title', {count: this.dataSource.selection.selected.length}),
        this.translate.instant('attribute.delete-attributes-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe((result) => {
        if (result) {
          this.attributeService.deleteEntityAttributes(this.entityIdValue,
            this.attributeScope as AttributeScope, this.dataSource.selection.selected).subscribe(
            () => {
              this.reloadAttributes();
            }
          );
        }
      });
    }
  }

  enterWidgetMode() {
    this.mode = 'widget';

    // TODO:
  }

  exitWidgetMode() {
    this.mode = 'default';
    this.reloadAttributes();

    // TODO:
  }

}
