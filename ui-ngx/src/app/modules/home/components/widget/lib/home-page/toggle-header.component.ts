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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ContentChildren, EventEmitter,
  Input,
  OnInit, Output,
  QueryList,
  ViewChild
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { UpdateMessage } from '@shared/models/settings.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of, Subscription } from 'rxjs';
import { MatStepper } from '@angular/material/stepper';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { coerceBoolean } from '@shared/decorators/coerce-boolean';

export interface ToggleHeaderOption {
  name: string;
  value: any;
}

@Component({
  selector: 'tb-toggle-header',
  templateUrl: './toggle-header.component.html',
  styleUrls: ['./toggle-header.component.scss']
})
export class ToggleHeaderComponent extends PageComponent implements OnInit {

  @Input()
  value: any;

  @Output()
  valueChange = new EventEmitter<any>();

  @Input()
  options: ToggleHeaderOption[];

  @Input()
  name: string;

  @Input()
  @coerceBoolean()
  useSelectOnMdLg = true;

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
}
