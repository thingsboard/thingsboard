// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Title } from '@angular/platform-browser';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { UtilsService } from '@core/services/utils.service';

@Injectable({
  providedIn: 'root'
})
export class TitleService {
  constructor(
    private translate: TranslateService,
    private menuService: MenuService,
    private utils: UtilsService,
    private whiteLabelingService: WhiteLabelingService,
    private title: Title
  ) {}

  setTitle(
    snapshot: ActivatedRouteSnapshot,
    lazyTranslate?: TranslateService
  ) {
    let lastChild = snapshot;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    const { title, customTitle, customChildTitle } = lastChild.data;

    let section: MenuSection = null;
    if (customTitle || customChildTitle) {
      section = customChildTitle ? this.menuService.getCurrentCustomChildSection() : this.menuService.getCurrentCustomSection();
    }
    if (section) {
      const customSectionTitle = this.utils.customTranslation(section.name, section.name);
      this.title.setTitle(`${this.whiteLabelingService.appTitle() || 'ThingsBoard'} | ${customSectionTitle}`);
    } else {
      const translate = lazyTranslate || this.translate;
      if (title) {
        translate
          .get(title)
          .pipe(filter(translatedTitle => translatedTitle !== title))
          .subscribe(translatedTitle =>
            this.title.setTitle(`${this.whiteLabelingService.appTitle() || 'ThingsBoard'} | ${translatedTitle}`)
          );
      } else {
        this.title.setTitle(this.whiteLabelingService.appTitle() || 'ThingsBoard');
      }
    }
  }
}
