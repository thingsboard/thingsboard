///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-menu-toggle',
  templateUrl: './menu-toggle.component.html',
  styleUrls: ['./menu-toggle.component.scss']
})
export class MenuToggleComponent implements OnInit {

  @Input() section: MenuSection;

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  sectionActive(): boolean {
    return this.router.isActive(this.section.path, false);
  }

  sectionHeight(): string {
    if (this.router.isActive(this.section.path, false)) {
      return this.section.height;
    } else {
      return '0px';
    }
  }

  trackBySectionPages(index: number, section: MenuSection){
    return section.id;
  }
}
