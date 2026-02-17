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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserSettingsService } from '@core/http/user-settings.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetContext } from '@home/models/widget-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  GettingStartedCompletedDialogComponent
} from '@home/components/widget/lib/home-page/getting-started-completed-dialog.component';
import { GettingStarted } from '@shared/models/user-settings.models';
import { CdkStep, StepperSelectionEvent } from '@angular/cdk/stepper';
import { baseUrl, isUndefined } from '@core/utils';
import { MatStepper } from '@angular/material/stepper';
import { first } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-getting-started-widget',
    templateUrl: './getting-started-widget.component.html',
    styleUrls: ['./getting-started-widget.component.scss'],
    standalone: false
})
export class GettingStartedWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('matStepper')
  matStepper: MatStepper;

  @Input()
  ctx: WidgetContext;

  authority = Authority;

  authUser = getCurrentAuthUser(this.store);
  gettingStarted: GettingStarted = {
    lastSelectedIndex: 0,
    maxSelectedIndex: 0
  };
  allCompleted = false;

  baseUrl = baseUrl();

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private userSettingsService: UserSettingsService,
              private dialog: MatDialog) {
    super(store);
  }

  ngOnInit() {
    this.userSettingsService.getGettingStarted().subscribe(
      (gettingStarted) => {
        if (gettingStarted) {
          this.gettingStarted = gettingStarted;
          if (isUndefined(this.gettingStarted.lastSelectedIndex)) {
            this.gettingStarted.lastSelectedIndex = 0;
          }
          if (isUndefined(this.gettingStarted.maxSelectedIndex)) {
            this.gettingStarted.maxSelectedIndex = 0;
          }
          if (this.gettingStarted.lastSelectedIndex > 0 && this.gettingStarted.lastSelectedIndex < this.matStepper.steps.length) {
            const animationDuration = this.matStepper.animationDuration;
            this.matStepper.animationDuration = '0';
            this.matStepper.selectedIndex = this.gettingStarted.lastSelectedIndex;
            this.matStepper.animationDone.pipe(first()).subscribe(() => {
              setTimeout(() => {this.matStepper.animationDuration = animationDuration;}, 0);
            });
          }
          if (this.matStepper.steps.length) {
            this.updateCompletedSteps();
            this.cd.markForCheck();
          } else {
            this.matStepper.steps.changes.subscribe(
              () => {
                this.updateCompletedSteps();
                this.cd.markForCheck();
              }
            );
          }
        }
      }
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  isSelected(step: CdkStep) {
    return this.matStepper?.selected === step;
  }

  gettingStartedCompleted() {
    this.dialog.open<GettingStartedCompletedDialogComponent, any,
      void>(GettingStartedCompletedDialogComponent, {
      disableClose: true,
      autoFocus: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe();
  }

  updateSelectedIndex(event: StepperSelectionEvent) {
    if (this.gettingStarted.lastSelectedIndex !== event.selectedIndex) {
      this.gettingStarted.lastSelectedIndex = event.selectedIndex;
      if (event.selectedIndex > this.gettingStarted.maxSelectedIndex) {
        this.gettingStarted.maxSelectedIndex = event.selectedIndex;
        this.updateCompletedSteps();
      }
      this.userSettingsService
        .updateGettingStarted(this.gettingStarted, {ignoreLoading: true}).subscribe();
    }
  }

  updateCompletedSteps() {
    if (this.gettingStarted.maxSelectedIndex >= this.matStepper.steps.length-1) {
      this.allCompleted = true;
    }
  }

}
