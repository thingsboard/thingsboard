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

import { PageComponent } from '@shared/components/page.component';
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormControl } from '@angular/forms';
import { BehaviorSubject, combineLatest, debounce, Observable, of, timer } from 'rxjs';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { getMaterialIcons, MaterialIcon } from '@shared/models/icon.models';
import { distinctUntilChanged, map, mergeMap, share, startWith, tap } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isTbImage } from '@shared/models/resource.models';

@Component({
    selector: 'tb-material-icons',
    templateUrl: './material-icons.component.html',
    providers: [],
    styleUrls: ['./material-icons.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class MaterialIconsComponent extends PageComponent implements OnInit {

  @ViewChild('iconsPanel')
  iconsPanel: CdkVirtualScrollViewport;

  @Input()
  selectedIcon: string;

  @Input()
  @coerceBoolean()
  iconClearButton = false;

  @Input()
  @coerceBoolean()
  showTitle = true;

  @Input()
  @coerceBoolean()
  allowedCustomIcon = false;

  @Input()
  popover: TbPopoverComponent;

  @Output()
  iconSelected = new EventEmitter<string>();

  iconRows$: Observable<MaterialIcon[][]>;
  showAllSubject = new BehaviorSubject<boolean>(false);
  searchIconControl: UntypedFormControl;

  isCustomIcon = false;

  iconsRowHeight = 48;

  iconsPanelHeight: string;
  iconsPanelWidth: string;

  notFound = false;

  constructor(protected store: Store<AppState>,
              private resourcesService: ResourcesService,
              private breakpointObserver: BreakpointObserver,
              private cd: ChangeDetectorRef) {
    super(store);
    this.searchIconControl = new UntypedFormControl('');
  }

  ngOnInit(): void {
    const iconsRowSize = this.breakpointObserver.isMatched(MediaBreakpoints['lt-md']) ? 8 : 11;
    this.calculatePanelSize(iconsRowSize);
    const iconsRowSizeObservable = this.breakpointObserver
      .observe(MediaBreakpoints['lt-md']).pipe(
        map((state) => state.matches ? 8 : 11),
        startWith(iconsRowSize),
    );
    this.iconRows$ = combineLatest({showAll: this.showAllSubject.asObservable(),
      rowSize: iconsRowSizeObservable,
      searchText: this.searchIconControl.valueChanges.pipe(
        startWith(''),
        debounce((searchText) => searchText ? timer(150) : of({})),
      )}).pipe(
      map((data) => {
        if (data.searchText && !data.showAll) {
          data.showAll = true;
          this.showAllSubject.next(true);
        }
        return data;
      }),
      distinctUntilChanged((p, c) => c.showAll === p.showAll && c.searchText === p.searchText && c.rowSize === p.rowSize),
      mergeMap((data) => getMaterialIcons(this.resourcesService, data.rowSize, data.showAll, data.searchText).pipe(
        map(iconRows => ({iconRows, iconsRowSize: data.rowSize}))
      )),
      tap((data) => {
        this.notFound = !data.iconRows.length;
        this.calculatePanelSize(data.iconsRowSize, data.iconRows.length);
        this.cd.markForCheck();
        setTimeout(() => {
          this.checkSize();
        }, 0);
      }),
      map((data) => data.iconRows),
      share()
    );
    this.isCustomIcon = isTbImage(this.selectedIcon)
  }

  clearSearch() {
    this.searchIconControl.patchValue('', {emitEvent: true});
  }

  selectIcon(icon: string) {
    this.iconSelected.emit(icon);
  }

  clearIcon() {
    this.iconSelected.emit(null);
  }

  private calculatePanelSize(iconsRowSize: number, iconRows = 4) {
    this.iconsPanelHeight = Math.min(iconRows * this.iconsRowHeight, 10 * this.iconsRowHeight) + 'px';
    this.iconsPanelWidth = (iconsRowSize * 36 + (iconsRowSize - 1) * 12 + 6) + 'px';
  }

  private checkSize() {
    this.iconsPanel?.checkViewportSize();
    this.popover?.updatePosition();
  }
}
