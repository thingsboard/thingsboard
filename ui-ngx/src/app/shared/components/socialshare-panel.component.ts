///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectAuthUser, selectUserDetails } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { Router } from '@angular/router';
import {isLocalUrl} from '@core/utils';

@Component({
  selector: 'tb-social-share-panel',
  templateUrl: './socialshare-panel.component.html',
  styleUrls: []
})
export class SocialSharePanelComponent implements OnInit {

  @Input()
  shareTitle: string;

  @Input()
  shareText: string;

  @Input()
  shareLink: string;

  @Input()
  shareHashTags: string;

  constructor() {
  }

  ngOnInit(): void {
  }

  isShareLinkLocal(): boolean {
    if (this.shareLink && this.shareLink.length > 0) {
      return isLocalUrl(this.shareLink);
    } else {
      return true;
    }
  }

}
