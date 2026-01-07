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

import { ChangeDetectorRef, Component, Inject, Input, OnInit, Type } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatasourceData, FormattedData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  createLabelFromPattern,
  flatDataWithoutOverride,
  formattedDataFormDatasourceData,
  hashCode,
  isDefinedAndNotNull,
  isNotEmptyStr,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';
import { HOME_COMPONENTS_MODULE_TOKEN, WIDGET_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { CompiledTbFunction, TbFunction } from '@shared/models/js-function.models';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

interface MarkdownWidgetSettings {
  markdownTextPattern: string;
  useMarkdownTextFunction: boolean;
  markdownTextFunction: TbFunction;
  applyDefaultMarkdownStyle: boolean;
  markdownCss: string;
}

type MarkdownTextFunction = (data: FormattedData[], ctx: WidgetContext) => string;

@Component({
  selector: 'tb-markdown-widget',
  templateUrl: './markdown-widget.component.html'
})
export class MarkdownWidgetComponent extends PageComponent implements OnInit {

  settings: MarkdownWidgetSettings;
  markdownTextFunction: Observable<CompiledTbFunction<MarkdownTextFunction>>;

  markdownClass: string;

  @Input()
  ctx: WidgetContext;

  data: FormattedData[];

  markdownText: string;

  additionalStyles: string[];

  applyDefaultMarkdownStyle = true;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              @Inject(HOME_COMPONENTS_MODULE_TOKEN) public homeComponentsModule: Type<any>,
              @Inject(WIDGET_COMPONENTS_MODULE_TOKEN) public widgetComponentsModule: Type<any>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.markdownWidget = this;
    this.settings = this.ctx.settings;
    this.markdownTextFunction = this.settings.useMarkdownTextFunction ?
      parseTbFunction(this.ctx.http, this.settings.markdownTextFunction, ['data', 'ctx']) : of(null);
    let cssString = this.settings.markdownCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      this.markdownClass = 'markdown-widget-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = this.markdownClass;
      cssParser.testMode = false;
      const cssObjects = cssParser.applyNamespacing(cssString);
      cssString = cssParser.getCSSForEditor(cssObjects);
      this.additionalStyles = [cssString];
    }
    if (isDefinedAndNotNull(this.settings.applyDefaultMarkdownStyle)) {
      this.applyDefaultMarkdownStyle = this.settings.applyDefaultMarkdownStyle;
    }
    const pageSize = isDefinedAndNotNull(this.ctx.widgetConfig.pageSize) &&
                      this.ctx.widgetConfig.pageSize > 0 ? this.ctx.widgetConfig.pageSize : 16384;
    const pageLink: EntityDataPageLink = {
      page: 0,
      pageSize,
      textSearch: null,
      dynamic: true
    };
    if (this.ctx.widgetConfig.datasources.length) {
      this.ctx.defaultSubscription.subscribeAllForPaginatedData(pageLink, null);
    } else {
      this.onDataUpdated();
    }
  }

  public onDataUpdated() {
    let initialData: DatasourceData[];
    if (this.ctx.data?.length) {
      initialData = this.ctx.data;
    } else if (this.ctx.datasources?.length) {
      initialData = [
        {
          datasource: this.ctx.datasources[0],
          dataKey: {
            type: DataKeyType.attribute,
            name: 'empty'
          },
          data: []
        }
      ];
    } else {
      initialData = [];
    }
    this.data = formattedDataFormDatasourceData(initialData);

    const markdownText = this.settings.useMarkdownTextFunction ?
      this.markdownTextFunction.pipe(map(markdownTextFunction => safeExecuteTbFunction(markdownTextFunction, [this.data, this.ctx]))) : this.settings.markdownTextPattern;
    if (typeof markdownText === 'string') {
      this.updateMarkdownText(markdownText, this.data);
    } else {
      markdownText.subscribe((text) => {
        this.updateMarkdownText(text, this.data);
      });
    }
  }

  private updateMarkdownText(markdownText: string, data: FormattedData[]) {
    const allData: FormattedData = flatDataWithoutOverride(data);
    markdownText = createLabelFromPattern(markdownText, allData);
    if (this.markdownText !== markdownText) {
      this.markdownText = this.utils.customTranslation(markdownText, markdownText);
    }
    this.cd.detectChanges();
  }

  markdownClick($event: MouseEvent) {
    this.ctx.actionsApi.elementClick($event);
  }

}
