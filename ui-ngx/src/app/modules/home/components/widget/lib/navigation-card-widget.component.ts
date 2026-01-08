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
import { Component, Input, NgZone, OnInit } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';

interface NavigationCardWidgetSettings {
  name: string;
  icon: string;
  path: string;
}

@Component({
  selector: 'tb-navigation-card-widget',
  templateUrl: './navigation-card-widget.component.html',
  styleUrls: ['./navigation-card-widget.component.scss']
})
export class NavigationCardWidgetComponent extends PageComponent implements OnInit {

  settings: NavigationCardWidgetSettings;

  translatedName: string;

  @Input()
  ctx: WidgetContext;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private ngZone: NgZone,
              private router: Router) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.navigationCardWidget = this;
    this.settings = this.ctx.settings;
    this.translatedName = this.utils.customTranslation(this.settings.name, this.settings.name);
  }


  navigate($event: Event, path: string) {
    $event.preventDefault();
    this.ngZone.run(() => {
      this.router.navigateByUrl(path);
    });
  }

}
