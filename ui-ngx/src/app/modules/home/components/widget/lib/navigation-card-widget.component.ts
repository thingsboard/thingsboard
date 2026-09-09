// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: ['./navigation-card-widget.component.scss'],
    standalone: false
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
