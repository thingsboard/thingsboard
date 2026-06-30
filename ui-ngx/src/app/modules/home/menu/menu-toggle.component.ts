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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionPreferencesUpdateOpenedMenuSection } from '@core/auth/auth.actions';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MenuService } from '@core/services/menu.service';

@Component({
    selector: 'tb-menu-toggle',
    templateUrl: './menu-toggle.component.html',
    styleUrls: ['./menu-toggle.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MenuToggleComponent {

  @Input() section: MenuSection;

  @Input()
  @coerceBoolean()
  collapsed = false;

  constructor(private store: Store<AppState>,
              private menuService: MenuService) {
  }

  sectionHeight(): string {
    if (this.section.opened && !this.collapsed) {
      return this.section.pages.length * 40 + 'px';
    } else {
      return '0px';
    }
  }

  toggleSection(event: MouseEvent) {
    event.stopPropagation();
    if (this.collapsed) {
      event.preventDefault();
    } else {
      this.section.opened = !this.section.opened;
      this.store.dispatch(new ActionPreferencesUpdateOpenedMenuSection({
        path: this.section.path,
        opened: this.section.opened
      }));
    }
  }

  toggleSectionActive(): boolean {
    if (this.collapsed) {
      return this.menuService.isActiveMenuSection(this.section);
    } else {
      return false;
    }
  }
}
