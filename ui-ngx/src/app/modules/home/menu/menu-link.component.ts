// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-menu-link',
    templateUrl: './menu-link.component.html',
    styleUrls: ['./menu-link.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MenuLinkComponent {

  @Input() section: MenuSection;

  @Input()
  @coerceBoolean()
  collapsed = false;

  constructor() {
  }

}
