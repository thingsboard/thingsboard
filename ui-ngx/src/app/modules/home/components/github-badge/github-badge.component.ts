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

import { Component, OnDestroy } from '@angular/core';
import { GitHubService } from '@core/http/git-hub.service';
import { Store } from '@ngrx/store';
import { selectAuthUser, selectIsAuthenticated } from '@core/auth/auth.selectors';
import { distinctUntilChanged, filter, map, switchMap, take, takeUntil } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { Subject } from 'rxjs';

const SETTINGS_KEY = 'HIDE_GITHUB_STAR_BUTTON';

@Component({
  selector: 'tb-github-badge',
  templateUrl: './github-badge.component.html',
  styleUrl: './github-badge.component.scss'
})
export class GithubBadgeComponent implements OnDestroy {

  githubStar = 0;

  private stopWatch$ = new Subject<void>();

  constructor(private gitHubService: GitHubService,
              private localStorageService: LocalStorageService,
              private store: Store<AppState>,) {
    const hide = this.localStorageService.getItem(SETTINGS_KEY) ?? false;

    if (!hide) {
      this.store.select(selectIsAuthenticated).pipe(
        filter((data) => data),
        switchMap(() => this.store.select(selectAuthUser).pipe(take(1))),
        map((authUser) => {
          return [Authority.TENANT_ADMIN, Authority.SYS_ADMIN].includes(authUser?.authority ?? Authority.ANONYMOUS)
        }),
        distinctUntilChanged(),
        takeUntil(this.stopWatch$),
      ).subscribe(value => {
        if (value) {
          this.gitHubService.getGitHubStar().subscribe(star => {
            this.githubStar = star;
          });
        } else {
          this.githubStar = 0
        }
      });
    }
  }

  hideGithubStar($event: Event) {
    $event?.stopPropagation();
    this.localStorageService.setItem(SETTINGS_KEY, true);
    this.githubStar = 0;

    this.stopWatch$.next();
    this.stopWatch$.complete();
  }

  ngOnDestroy() {
    this.stopWatch$.next();
    this.stopWatch$.complete();
  }
}
