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

import { PageComponent } from '@shared/components/page.component';
import { Input, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext, IDynamicWidgetComponent } from '@home/models/widget-component.models';
import { ExceptionData } from '@shared/models/error.models';

export abstract class DynamicWidgetComponentModule implements OnDestroy {

  ngOnDestroy(): void {
    console.log('Module destroyed!');
  }

}

export abstract class DynamicWidgetComponent extends PageComponent implements IDynamicWidgetComponent, OnInit, OnDestroy {

  @Input()
  widgetContext: WidgetContext;

  @Input()
  widgetErrorData: ExceptionData;

  @Input()
  loadingData: boolean;

  [key: string]: any;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {

  }

  ngOnDestroy(): void {
    console.log('Component destroyed!');
  }

  clearRpcError() {
    if (this.widgetContext.defaultSubscription) {
      this.widgetContext.defaultSubscription.clearRpcError();
    }
  }

}
