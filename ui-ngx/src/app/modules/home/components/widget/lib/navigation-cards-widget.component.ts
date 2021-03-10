///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { MenuService } from '@core/services/menu.service';
import { HomeSection, HomeSectionPlace } from '@core/services/menu.models';
import { Router } from '@angular/router';
import { map } from 'rxjs/operators';

interface NavigationCardsWidgetSettings {
  filterType: 'all' | 'include' | 'exclude';
  filter: string[];
}

@Component({
  selector: 'tb-navigation-cards-widget',
  templateUrl: './navigation-cards-widget.component.html',
  styleUrls: ['./navigation-cards-widget.component.scss']
})
export class NavigationCardsWidgetComponent extends PageComponent implements OnInit {

  homeSections$ = this.menuService.homeSections();
  showHomeSections$ = this.homeSections$.pipe(
    map((sections) => {
      return sections.filter((section) => this.sectionPlaces(section).length > 0);
    })
  );

  cols = null;

  settings: NavigationCardsWidgetSettings;

  @Input()
  ctx: WidgetContext;

  constructor(protected store: Store<AppState>,
              private menuService: MenuService,
              private ngZone: NgZone,
              private router: Router) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.navigationCardsWidget = this;
    this.settings = this.ctx.settings;
  }

  resize() {
    this.updateColumnCount();
  }

  private updateColumnCount() {
    this.cols = 2;
    const width = this.ctx.width;
    if (width >= 1280) {
      this.cols = 3;
      if (width >= 1920) {
        this.cols = 4;
      }
    }
    this.ctx.detectChanges();
  }

  navigate($event: Event, path: string) {
    $event.preventDefault();
    this.ngZone.run(() => {
      this.router.navigateByUrl(path);
    });
  }

  sectionPlaces(section: HomeSection): HomeSectionPlace[] {
    return section && section.places ? section.places.filter((place) => this.filterPlace(place)) : [];
  }

  private filterPlace(place: HomeSectionPlace): boolean {
    if (this.settings.filterType === 'include') {
      return this.settings.filter.includes(place.path);
    } else if (this.settings.filterType === 'exclude') {
      return !this.settings.filter.includes(place.path);
    }
    return true;
  }

  sectionColspan(section: HomeSection): number {
    if (this.ctx.width >= 960) {
      let colspan = this.cols;
      const places = this.sectionPlaces(section);
      if (places.length <= colspan) {
        colspan = places.length;
      }
      return colspan;
    } else {
      return 2;
    }
  }

}
