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
  EntityRelation,
  EntityRelationInfo,
  EntitySearchDirection,
  entitySearchDirectionTranslations,
  RelationTypeGroup
} from '@shared/models/relation.models';
import { EntityId } from '@shared/models/id/entity-id';
import { RelationsDatasource } from '../../models/datasource/relation-datasource';
import { RelationDialogComponent, RelationDialogData } from '@home/components/relation/relation-dialog.component';
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

  directions = EntitySearchDirection;

  directionTypes = Object.keys(EntitySearchDirection);

  directionTypeTranslations = entitySearchDirectionTranslations;

  displayedColumns: string[];
  direction: EntitySearchDirection;
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
              private dialogService: DialogService,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private fb: FormBuilder,
              private zone: NgZone) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'type', direction: Direction.ASC };
    this.direction = EntitySearchDirection.FROM;
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new RelationsDatasource(this.entityRelationService, this.translate);
    this.updateColumns();
  }

  ngOnInit() {
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
    if (this.direction === EntitySearchDirection.FROM) {
      this.displayedColumns = ['select', 'type', 'toEntityTypeName', 'toName', 'actions'];
    } else {
      this.displayedColumns = ['select', 'type', 'fromEntityTypeName', 'fromName', 'actions'];
    }
  }

  directionChanged(direction: EntitySearchDirection) {
    this.direction = direction;
    this.updateColumns();
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
    this.direction = EntitySearchDirection.FROM;
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

  deleteRelation($event: Event, relation: EntityRelationInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    let title;
    let content;
    if (this.direction === EntitySearchDirection.FROM) {
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

      if (this.direction === EntitySearchDirection.FROM) {
        title = this.translate.instant('relation.delete-to-relations-title', {count: this.dataSource.selection.selected.length});
        content = this.translate.instant('relation.delete-to-relations-text');
      } else {
        title = this.translate.instant('relation.delete-from-relations-title', {count: this.dataSource.selection.selected.length});
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

  openRelationDialog($event: Event, relation: EntityRelation = null) {
    if ($event) {
      $event.stopPropagation();
    }

    let isAdd = false;
    if (!relation) {
      isAdd = true;
      relation = {
        from: null,
        to: null,
        type: null,
        typeGroup: RelationTypeGroup.COMMON
      };
      if (this.direction === EntitySearchDirection.FROM) {
        relation.from = this.entityIdValue;
      } else {
        relation.to = this.entityIdValue;
      }
    }

    this.dialog.open<RelationDialogComponent, RelationDialogData, boolean>(RelationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        direction: this.direction,
        relation: {...relation}
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
