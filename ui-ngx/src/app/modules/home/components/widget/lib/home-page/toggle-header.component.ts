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
  ContentChildren,
  Input,
  OnInit,
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
import { of } from 'rxjs';
import { MatStepper } from '@angular/material/stepper';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';

@Component({
  selector: 'tb-toggle-header',
  templateUrl: './toggle-header.component.html',
  styleUrls: ['./toggle-header.component.scss']
})
export class ToggleHeaderComponent extends PageComponent implements OnInit, AfterViewInit {

  @ViewChild('toggleGroup')
  toggleGroup: MatButtonToggleGroup;

  @ContentChildren(MatButtonToggle)
  _buttonToggles: QueryList<MatButtonToggle>;

  innerValue: any;

  @Input()
  set value(value: any) {
    this.innerValue = value;
  }

  get value(): any {
    return this.toggleGroup?.value;
  }

  @Input()
  name: string;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit() {
    for (const toggle of this._buttonToggles) {
      toggle.buttonToggleGroup = this.toggleGroup;
      if (this.innerValue === toggle.value) {
        toggle.checked = true;
      }
    }
  }

}
