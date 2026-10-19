// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { EntityTableHeaderComponent } from '@home/components/entity/entity-table-header.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AiModel } from '@shared/models/ai-model.models';

@Component({
    selector: 'tb-ai-model-table-header',
    templateUrl: './ai-model-table-header.component.html',
    styles: [`
    :host {
        width: 100%;
    }
  `],
    styleUrls: [],
    standalone: false
})
export class AiModelTableHeaderComponent extends EntityTableHeaderComponent<AiModel> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }
}
