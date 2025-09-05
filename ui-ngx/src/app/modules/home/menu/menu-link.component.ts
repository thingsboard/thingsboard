///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ChangeDetectionStrategy, Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { tbImageIcon } from '@shared/models/custom-menu.models';

@Component({
  selector: 'tb-menu-link',
  templateUrl: './menu-link.component.html',
  styleUrls: ['./menu-link.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuLinkComponent implements OnInit, OnChanges {

  @Input() section: MenuSection;

  isCustomIcon: boolean;

  constructor() {
  }

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'section' && change.currentValue) {
          this.isCustomIcon = tbImageIcon(change.currentValue.icon);
        }
      }
    }
  }

}
