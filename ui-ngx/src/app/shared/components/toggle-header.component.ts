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
  AfterContentChecked,
  AfterContentInit,
  AfterViewChecked,
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ContentChildren,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BehaviorSubject, Observable, ReplaySubject, Subject, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { startWith, takeUntil } from 'rxjs/operators';
import { Platform } from '@angular/cdk/platform';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

export interface ToggleHeaderOption {
  name: string;
  value: any;
  error$?: Observable<string>;
}

export type ToggleHeaderAppearance = 'fill' | 'fill-invert' | 'stroked';

export type ScrollDirection = 'after' | 'before';

@Directive(
  {
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tb-toggle-option',
  }
)
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ToggleOption implements OnChanges, OnDestroy {

  @Input() value: any;

  @Input() error: string;

  currentError = new ReplaySubject<string>(1);

  get viewValue(): string {
    return (this._element?.nativeElement.textContent || '').trim();
  }

  constructor(
    private _element: ElementRef<HTMLElement>
  ) {}

  ngOnChanges(changes: SimpleChanges) {
    if (changes?.error) {
      if (changes.error.currentValue !== changes.error.previousValue) {
        this.currentError.next(this.error);
      }
    }
  }

  ngOnDestroy() {
    this.currentError.complete();
  }
}

@Directive()
export abstract class _ToggleBase extends PageComponent implements AfterContentInit, OnDestroy {

  @ContentChildren(ToggleOption) toggleOptions: QueryList<ToggleOption>;

  @Input()
  options: ToggleHeaderOption[] = [];

  protected _destroyed = new Subject<void>();

  protected constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngAfterContentInit(): void {
    this.toggleOptions.changes.pipe(startWith(null), takeUntil(this._destroyed)).subscribe(() => {
      this.syncToggleHeaderOptions();
    });
  }

  ngOnDestroy() {
    this._destroyed.next();
    this._destroyed.complete();
  }

  private syncToggleHeaderOptions() {
    if (this.toggleOptions?.length) {
      this.options.length = 0;
      this.toggleOptions.forEach(option => {
        this.options.push(
          {
            name: option.viewValue,
            value: option.value,
            error$: option.currentError.asObservable()
          }
        );
      });
    }
  }

}

@Component({
  selector: 'tb-toggle-header',
  templateUrl: './toggle-header.component.html',
  styleUrls: ['./toggle-header.component.scss']
})
export class ToggleHeaderComponent extends _ToggleBase implements OnInit, AfterViewInit, AfterContentInit,
  AfterContentChecked, AfterViewChecked, OnDestroy {

  @ViewChild('toggleGroup', {static: false})
  toggleGroup: ElementRef<HTMLElement>;

  @ViewChild(MatButtonToggleGroup, {static: false})
  buttonToggleGroup: MatButtonToggleGroup;

  @ViewChild('toggleGroupContainer', {static: false})
  toggleGroupContainer: ElementRef<HTMLElement>;

  @HostBinding('class.tb-toggle-header-pagination-controls-enabled')
  private showPaginationControls = false;
  private _showPaginationControlsChanged = false;

  private toggleGroupResize$: ResizeObserver;

  leftPaginationEnabled = false;
  rightPaginationEnabled = false;

  private _scrollDistance = 0;
  private _scrollDistanceChanged: boolean;

  get scrollDistance(): number {
    return this._scrollDistance;
  }
  set scrollDistance(value: number) {
    this._scrollTo(value);
  }

  @Input()
  value: any;

  @Output()
  valueChange = new EventEmitter<any>();

  @Input()
  name: string;

  @Input()
  @coerceBoolean()
  disablePagination = false;

  @Input()
  selectMediaBreakpoint = 'md-lg';

  @Input()
  @coerceBoolean()
  set useSelectOnMdLg(value: boolean) {
    if (value) {
      this.selectMediaBreakpoint = 'md-lg';
    } else {
      if (this.selectMediaBreakpoint === 'md-lg') {
        this.selectMediaBreakpoint = '';
      }
    }
  }

  @Input()
  @coerceBoolean()
  ignoreMdLgSize = false;

  @Input()
  appearance: ToggleHeaderAppearance = 'stroked';

  @Input()
  @coerceBoolean()
  disabled = false;

  @Input()
  @coerceBoolean()
  fillHeight = false;

  @Input()
  @coerceBoolean()
  extraPadding = false;

  @Input()
  @coerceBoolean()
  primaryBackground = false;

  get isMdLg(): boolean {
    return !this.ignoreMdLgSize && this.isMdLgValue;
  }

  private isMdLgValue: boolean;
  private useSelectSubject = new BehaviorSubject(false);

  useSelect$ = this.useSelectSubject.asObservable();

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private platform: Platform,
              private breakpointObserver: BreakpointObserver,
              private zone: NgZone) {
    super(store);
  }

  ngOnInit() {
    const mediaBreakpoints = [MediaBreakpoints['md-lg']];
    if (this.selectMediaBreakpoint && this.selectMediaBreakpoint !== 'md-lg') {
      mediaBreakpoints.push(MediaBreakpoints[this.selectMediaBreakpoint]);
    }
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe(mediaBreakpoints)
      .subscribe((state: BreakpointState) => {
          this.isMdLgValue = state.breakpoints[MediaBreakpoints['md-lg']];
          if (this.selectMediaBreakpoint) {
            this.useSelectSubject.next(state.breakpoints[MediaBreakpoints[this.selectMediaBreakpoint]]);
          } else {
            this.useSelectSubject.next(false);
          }
          this.cd.markForCheck();
        }
      );
    if (!this.disablePagination) {
      this.valueChange.pipe(takeUntil(this._destroyed)).subscribe(() => {
        this.scrollToToggleOptionValue();
      });
    }
  }

  ngOnDestroy() {
    this.stopObservePagination();
    super.ngOnDestroy();
  }

  ngAfterViewInit() {
    if (!this.disablePagination) {
      this.useSelect$.pipe(takeUntil(this._destroyed)).subscribe((useSelect) => {
        if (useSelect) {
          this.removePagination();
        } else {
          setTimeout(() => {
            this.startObservePagination();
          }, 0);
        }
      });
    }
  }

  ngAfterContentChecked() {
    if (this._scrollDistanceChanged) {
      this.updateToggleHeaderScrollPosition();
      this._scrollDistanceChanged = false;
      this.cd.markForCheck();
    }
  }

  ngAfterViewChecked() {
    if (this._showPaginationControlsChanged) {
      this.scrollToToggleOptionValue();
      this._showPaginationControlsChanged = false;
      this.cd.markForCheck();
    }
  }

  trackByHeaderOption(index: number, option: ToggleHeaderOption){
    return option.value;
  }

  handlePaginatorClick(direction: ScrollDirection, $event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.scrollHeader(direction);
  }

  handlePaginatorTouchStart(direction: ScrollDirection, $event: Event) {
    if (direction === 'before' && !this.leftPaginationEnabled ||
        direction === 'after' && !this.rightPaginationEnabled) {
      $event.preventDefault();
    }
  }

  private startObservePagination() {
    this.toggleGroupResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        this.updatePagination();
      });
    });
    this.toggleGroupResize$.observe(this.toggleGroupContainer.nativeElement);
  }

  private removePagination() {
    this.stopObservePagination();
    this.showPaginationControls = false;
  }

  private stopObservePagination() {
    if (this.toggleGroupResize$) {
      this.toggleGroupResize$.disconnect();
      this.toggleGroupResize$ = null;
    }
  }

  private scrollHeader(direction: ScrollDirection) {
    const viewLength = this.toggleGroup.nativeElement.offsetWidth;
    // Move the scroll distance one-third the length of the tab list's viewport.
    const scrollAmount = ((direction === 'before' ? -1 : 1) * viewLength) / 3;
    return this._scrollTo(this._scrollDistance + scrollAmount);
  }

  private scrollToToggleOptionValue() {
    if (this.buttonToggleGroup && this.buttonToggleGroup.selected) {
      const selectedToggleButton = this.buttonToggleGroup.selected as MatButtonToggle;
      const index = this.options.findIndex(o => o.value === selectedToggleButton.value);
      const isLast = index === this.options.length - 1;
      const isFirst = index === 0;
      const viewLength = this.toggleGroupContainer.nativeElement.offsetWidth;
      const {offsetLeft, offsetWidth} = (selectedToggleButton._buttonElement.nativeElement.offsetParent as HTMLElement);
      const labelBeforePos = isFirst ? 0 : offsetLeft;
      const labelAfterPos = isLast ? this.toggleGroup.nativeElement.scrollWidth : labelBeforePos + offsetWidth;
      const beforeVisiblePos = this.scrollDistance;
      const afterVisiblePos = this.scrollDistance + viewLength;
      if (labelBeforePos < beforeVisiblePos) {
        this.scrollDistance -= beforeVisiblePos - labelBeforePos;
      } else if (labelAfterPos > afterVisiblePos) {
        this.scrollDistance += Math.min(
          labelAfterPos - afterVisiblePos,
          labelBeforePos - beforeVisiblePos,
        );
      }
    }
  }

  private updatePagination() {
    this.checkPaginationEnabled();
    this.checkPaginationControls();
    this.updateToggleHeaderScrollPosition();
  }

  private checkPaginationEnabled() {
    if (this.toggleGroupContainer) {
      const isEnabled = this.toggleGroup.nativeElement.scrollWidth > this.toggleGroupContainer.nativeElement.offsetWidth;
      if (isEnabled !== this.showPaginationControls) {
        if (!isEnabled) {
          this.scrollDistance = 0;
        } else {
          this._showPaginationControlsChanged = true;
        }
        this.cd.markForCheck();
        this.showPaginationControls = isEnabled;
      }
    } else {
      this.showPaginationControls = false;
    }
  }

  private checkPaginationControls() {
    if (!this.showPaginationControls) {
      this.leftPaginationEnabled = this.rightPaginationEnabled = false;
    } else {
      // Check if the pagination arrows should be activated.
      this.leftPaginationEnabled = this.scrollDistance > 0;
      this.rightPaginationEnabled = this.scrollDistance < this.getMaxScrollDistance();
      this.cd.markForCheck();
    }
  }

  private getMaxScrollDistance(): number {
    const lengthOfToggleGroup = this.toggleGroup.nativeElement.scrollWidth;
    const viewLength = this.toggleGroupContainer.nativeElement.offsetWidth;
    return lengthOfToggleGroup - viewLength || 0;
  }

  private _scrollTo(position: number) {
    if (!this.showPaginationControls) {
      return {maxScrollDistance: 0, distance: 0};
    } else {
      const maxScrollDistance = this.getMaxScrollDistance();
      this._scrollDistance = Math.max(0, Math.min(maxScrollDistance, position));
      this._scrollDistanceChanged = true;
      this.checkPaginationControls();
      return {maxScrollDistance, distance: this._scrollDistance};
    }
  }

  private updateToggleHeaderScrollPosition() {
    if (this.toggleGroupContainer) {
      const scrollDistance = this.scrollDistance;
      const translateX = -scrollDistance;
      this.toggleGroup.nativeElement.style.transform = `translateX(${Math.round(translateX)}px)`;
      if (this.platform.TRIDENT || this.platform.EDGE) {
        this.toggleGroupContainer.nativeElement.scrollLeft = 0;
      }
    }
  }
}
