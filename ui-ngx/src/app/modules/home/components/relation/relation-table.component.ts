///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
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
import { forkJoin, fromEvent, merge, Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
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

@Component({
  selector: 'tb-relation-table',
  templateUrl: './relation-table.component.html',
  styleUrls: ['./relation-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RelationTableComponent extends PageComponent implements AfterViewInit, OnInit {

  directions = EntitySearchDirection;

  directionTypes = Object.keys(EntitySearchDirection);

  directionTypeTranslations = entitySearchDirectionTranslations;

  displayedColumns: string[];
  direction: EntitySearchDirection;
  pageLink: PageLink;
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

  constructor(protected store: Store<AppState>,
              private entityRelationService: EntityRelationService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private dialogService: DialogService) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'type', direction: Direction.ASC };
    this.direction = EntitySearchDirection.FROM;
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new RelationsDatasource(this.entityRelationService, this.translate);
    this.updateColumns();
  }

  ngOnInit() {
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
    this.dataSource.loadRelations(this.direction, this.entityIdValue, this.pageLink, reload);
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
    this.direction = EntitySearchDirection.FROM;
    this.updateColumns();
    this.pageLink.textSearch = null;
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
