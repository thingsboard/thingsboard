// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';

@Component({
    selector: 'tb-side-menu',
    templateUrl: './side-menu.component.html',
    styleUrls: ['./side-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SideMenuComponent implements OnInit {

  menuSections$ = this.menuService.menuSections();

  constructor(private menuService: MenuService) {
  }

  trackByMenuSection(index: number, section: MenuSection){
    return section.id;
  }

  ngOnInit() {
  }

}
