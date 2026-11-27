// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input, NgZone,
  OnDestroy,
  OnInit,
  ViewChild
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
import { EntityRelationService } from '@core/http/entity-relation.service';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { forkJoin, merge, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import {
  ALL_RELATIONS_DIRECTION,
  EntityRelation,
  EntityRelationInfo,
  EntitySearchDirection,
  RelationsTableDirection,
  relationsTableDirectionTranslations,
  relationsTableEntityNameHeaderTranslations,
  relationsTableEntityTypeHeaderTranslations,
  RelationTypeGroup
} from '@shared/models/relation.models';
import { EntityId } from '@shared/models/id/entity-id';
import { RelationsDatasource } from '../../models/datasource/relation-datasource';
import { RelationDialogComponent, RelationDialogData } from '@home/components/relation/relation-dialog.component';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Operation, resourceByEntityType } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { FormBuilder } from '@angular/forms';

@Component({
    selector: 'tb-relation-table',
    templateUrl: './relation-table.component.html',
    styleUrls: ['./relation-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class RelationTableComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy {


  directionTypes: RelationsTableDirection[] = [ALL_RELATIONS_DIRECTION, EntitySearchDirection.FROM, EntitySearchDirection.TO];

  directionTypeTranslations = relationsTableDirectionTranslations;
  entityTypeHeaderTranslations = relationsTableEntityTypeHeaderTranslations;
  entityNameHeaderTranslations = relationsTableEntityNameHeaderTranslations;

  displayedColumns: string[];
  direction: RelationsTableDirection;
  pageLink: PageLink;
  hidePageSize = false;
  textSearchMode = false;
  dataSource: RelationsDatasource;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

  viewsInited = false;

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
      if (this.viewsInited) {
        this.resetSortAndFilter(this.activeValue);
        if (!this.activeValue) {
          this.dirtyValue = true;
        }
      }
    }
  }

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }

  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  textSearch = this.fb.control('', {nonNullable: true});

  private widgetResize$: ResizeObserver;
  private destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private entityRelationService: EntityRelationService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private userPermissionsService: UserPermissionsService,
              private dialogService: DialogService,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private fb: FormBuilder,
              private zone: NgZone) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'type', direction: Direction.ASC };
    this.direction = ALL_RELATIONS_DIRECTION;
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new RelationsDatasource(this.entityRelationService, this.translate);
  }

  ngOnInit() {
    this.updateColumns();
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

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  updateColumns() {
    this.displayedColumns = ['type', 'entityTypeName', 'entityName', 'actions'];
    if (this.direction === ALL_RELATIONS_DIRECTION) {
      this.displayedColumns.unshift('directionName');
    }
    if (!this.readonly) {
      this.displayedColumns.unshift('select');
    }
  }

  directionChanged(direction: RelationsTableDirection) {
    this.direction = direction;
    this.updateColumns();
    if (this.sort.active === 'directionName' && direction !== ALL_RELATIONS_DIRECTION) {
      this.sort.active = 'type';
      this.sort.direction = 'asc';
    }
    this.paginator.pageIndex = 0;
    this.updateData(true);
  }

  ngAfterViewInit() {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (this.pageLink.textSearch ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.paginator.pageIndex = 0;
      this.pageLink.textSearch = value.trim();
      this.updateData();
    });

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateData());

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
    this.dataSource.loadRelations(this.direction, this.entityIdValue, this.pageLink, reload);
  }

  enterFilterMode() {
    this.textSearchMode = true;
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  resetSortAndFilter(update: boolean = true) {
    this.direction = ALL_RELATIONS_DIRECTION;
    this.updateColumns();
    this.pageLink.textSearch = null;
    this.textSearch.reset('', {emitEvent: false});
    this.paginator.pageIndex = 0;
    const sortable = this.sort.sortables.get('type');
    this.sort.active = sortable.id;
    this.sort.direction = 'asc';
    if (update) {
      this.updateData(true);
    }
  }

  reloadRelations() {
    this.updateData(true);
  }

  addRelation($event: Event) {
    this.openRelationDialog($event);
  }

  editRelation($event: Event, relation: EntityRelationInfo) {
    this.openRelationDialog($event, relation);
  }
  showRelation($event: Event, relation: EntityRelationInfo) {
    this.openRelationDialog($event, relation, this.readonly);
  }

  isRelationEditable(relation: EntityRelationInfo): boolean {
    if (this.readonly) {
      return false;
    }
    const entityType = relation.direction === EntitySearchDirection.FROM ? relation.to.entityType : relation.from.entityType;
    const resource = resourceByEntityType.get(entityType as EntityType);
    return this.userPermissionsService.hasGenericPermission(resource, Operation.WRITE);
  }

  onRowClick($event: Event, groupPermission) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.readonly) {
      this.dataSource.selection.toggle(groupPermission);
    }
  }

  deleteRelation($event: Event, relation: EntityRelationInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    let title;
    let content;
    if (relation.direction === EntitySearchDirection.FROM) {
      title = this.translate.instant('relation.delete-to-relation-title', {entityName: relation.toName});
      content = this.translate.instant('relation.delete-to-relation-text', {entityName: relation.toName});
    } else {
      title = this.translate.instant('relation.delete-from-relation-title', {entityName: relation.fromName});
      content = this.translate.instant('relation.delete-from-relation-text', {entityName: relation.fromName});
    }

    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.entityRelationService.deleteRelation(
          relation.from,
          relation.type,
          relation.to
        ).subscribe(
          () => {
            this.reloadRelations();
          }
        );
      }
    });
  }

  deleteRelations($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.dataSource.selection.selected.length > 0) {
      let title;
      let content;
      const count = this.dataSource.selection.selected.length;
      const directions = new Set(this.dataSource.selection.selected.map(relation => relation.direction));

      if (directions.size > 1) {
        title = this.translate.instant('relation.delete-relations-title', {count});
        content = this.translate.instant('relation.delete-relations-text');
      } else if (directions.has(EntitySearchDirection.FROM)) {
        title = this.translate.instant('relation.delete-to-relations-title', {count});
        content = this.translate.instant('relation.delete-to-relations-text');
      } else {
        title = this.translate.instant('relation.delete-from-relations-title', {count});
        content = this.translate.instant('relation.delete-from-relations-text');
      }

      this.dialogService.confirm(
        title,
        content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe((result) => {
        if (result) {
          const tasks: Observable<any>[] = [];
          this.dataSource.selection.selected.forEach((relation) => {
            tasks.push(this.entityRelationService.deleteRelation(
              relation.from,
              relation.type,
              relation.to
            ));
          });
          forkJoin(tasks).subscribe(
            () => {
              this.reloadRelations();
            }
          );
        }
      });
    }
  }

  openRelationDialog($event: Event, relation: EntityRelationInfo = null, readonly: boolean = false) {
    if ($event) {
      $event.stopPropagation();
    }

    let isAdd = false;
    let direction: EntitySearchDirection;
    let dialogRelation: EntityRelation;
    if (!relation) {
      isAdd = true;
      direction = this.direction === EntitySearchDirection.TO ? EntitySearchDirection.TO : EntitySearchDirection.FROM;
      dialogRelation = {
        from: null,
        to: null,
        type: null,
        typeGroup: RelationTypeGroup.COMMON
      };
      if (direction === EntitySearchDirection.FROM) {
        dialogRelation.from = this.entityIdValue;
      } else {
        dialogRelation.to = this.entityIdValue;
      }
    } else {
      direction = relation.direction;
      dialogRelation = {...relation};
    }

    this.dialog.open<RelationDialogComponent, RelationDialogData, boolean>(RelationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        direction,
        relation: dialogRelation,
        readonly,
        directionSelectable: this.direction === ALL_RELATIONS_DIRECTION
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reloadRelations();
        }
      }
    );
  }

}
