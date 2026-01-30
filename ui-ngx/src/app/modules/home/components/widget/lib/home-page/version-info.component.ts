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

import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AdminService } from '@core/http/admin.service';
import { UpdateMessage } from '@shared/models/settings.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { of } from 'rxjs';

@Component({
    selector: 'tb-version-info',
    templateUrl: './version-info.component.html',
    styleUrls: ['./home-page-widget.scss', './version-info.component.scss'],
    standalone: false
})
export class VersionInfoComponent extends PageComponent implements OnInit {

  authUser = getCurrentAuthUser(this.store);
  updateMessage: UpdateMessage;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private adminService: AdminService) {
    super(store);
  }

  ngOnInit() {
    (this.authUser.authority === Authority.SYS_ADMIN ?
      this.adminService.checkUpdates() : of(null)).subscribe(
      (updateMessage) => {
        this.updateMessage = updateMessage;
        this.cd.markForCheck();
      }
    );
  }
}
