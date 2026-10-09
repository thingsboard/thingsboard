// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
    selector: 'tb-menu-link',
    templateUrl: './menu-link.component.html',
    styleUrls: [],
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
