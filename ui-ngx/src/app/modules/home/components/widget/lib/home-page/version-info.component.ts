// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
