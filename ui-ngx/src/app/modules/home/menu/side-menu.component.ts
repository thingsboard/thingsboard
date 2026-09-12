// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-side-menu',
    templateUrl: './side-menu.component.html',
    styleUrls: ['./side-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class SideMenuComponent {

  @Input()
  @coerceBoolean()
  collapsed = false;

  menuSections$ = this.menuService.menuSections();

  constructor(private menuService: MenuService) {
  }

}
