///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  AfterContentInit,
  ChangeDetectorRef,
  Component,
  ContentChildren,
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subject, Subscription } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coercion';
import { startWith, takeUntil } from 'rxjs/operators';

export interface ToggleHeaderOption {
  name: string;
  value: any;
}

export type ToggleHeaderAppearance = 'fill' | 'fill-invert' | 'stroked';

@Directive(
  {
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tb-toggle-option',
  }
)
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class ToggleOption {

  @Input() value: any;

  get viewValue(): string {
    return (this._element?.nativeElement.textContent || '').trim();
  }

  constructor(
    private _element: ElementRef<HTMLElement>
  ) {}
}

@Directive()
export abstract class _ToggleBase extends PageComponent implements AfterContentInit, OnDestroy {

  @ContentChildren(ToggleOption) toggleOptions: QueryList<ToggleOption>;

  @Input()
  options: ToggleHeaderOption[] = [];

  private _destroyed = new Subject<void>();

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
          { name: option.viewValue,
            value: option.value
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
export class ToggleHeaderComponent extends _ToggleBase implements OnInit, AfterContentInit, OnDestroy {

  @Input()
  value: any;

  @Output()
  valueChange = new EventEmitter<any>();

  @Input()
  name: string;

  @Input()
  @coerceBoolean()
  useSelectOnMdLg = true;

  @Input()
  @coerceBoolean()
  ignoreMdLgSize = false;

  @Input()
  appearance: ToggleHeaderAppearance = 'stroked';

  isMdLg: boolean;

  private observeBreakpointSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private breakpointObserver: BreakpointObserver) {
    super(store);
  }

  ngOnInit() {
    this.isMdLg = this.breakpointObserver.isMatched(MediaBreakpoints['md-lg']);
    this.observeBreakpointSubscription = this.breakpointObserver
      .observe(MediaBreakpoints['md-lg'])
      .subscribe((state: BreakpointState) => {
          this.isMdLg = state.matches;
          this.cd.markForCheck();
        }
      );
  }

  trackByHeaderOption(index: number, option: ToggleHeaderOption){
    return option.value;
  }
}
