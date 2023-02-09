///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { AttributeService } from '@core/http/attribute.service';
import { Observable } from 'rxjs';
import { initialUserPreferences, UserPreferences } from '@shared/models/user-preferences.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { map } from 'rxjs/operators';
import { AuthUser } from '@shared/models/user.model';

const USER_PREFERENCES_ATTRIBUTE_KEY = 'user_preferences';

@Injectable({
  providedIn: 'root'
})
export class UserPreferencesService {

  constructor(
    private store: Store<AppState>,
    private attributeService: AttributeService
  ) {}

  public loadUserPreferences(authUser: AuthUser): Observable<UserPreferences> {
    return this.attributeService.getEntityAttributes({id: authUser.userId, entityType: EntityType.USER},
      AttributeScope.SERVER_SCOPE, [USER_PREFERENCES_ATTRIBUTE_KEY], { ignoreLoading: true, ignoreErrors: true }).pipe(
        map((attributes) => {
          const found = ((attributes || []).find(data => data.key === USER_PREFERENCES_ATTRIBUTE_KEY));
          return found ? JSON.parse(found.value) : initialUserPreferences;
        })
    );
  }

  public saveUserPreferences(authUser: AuthUser, userPreferences: UserPreferences): Observable<void> {
    return this.attributeService.saveEntityAttributes({id: authUser.userId, entityType: EntityType.USER},
      AttributeScope.SERVER_SCOPE,
      [{key: USER_PREFERENCES_ATTRIBUTE_KEY, value: JSON.stringify(userPreferences)}],
      { ignoreLoading: true, ignoreErrors: true });
  }

}
