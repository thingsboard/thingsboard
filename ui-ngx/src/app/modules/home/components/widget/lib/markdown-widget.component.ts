///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, HostBinding, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatasourceData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  fillPattern, flatData,
  parseData,
  parseFunction,
  processPattern,
  safeExecute
} from '@home/components/widget/lib/maps/common-maps-utils';
import { FormattedData } from '@home/components/widget/lib/maps/map-models';
import { hashCode, isNotEmptyStr } from '@core/utils';
import cssjs from '@core/css/css';
import { UtilsService } from '@core/services/utils.service';

interface MarkdownWidgetSettings {
  markdownTextPattern: string;
  useMarkdownTextFunction: boolean;
  markdownTextFunction: string;
  markdownCss: string;
}

type MarkdownTextFunction = (data: FormattedData[]) => string;

@Component({
  selector: 'tb-markdown-widget ',
  templateUrl: './markdown-widget.component.html'
})
export class MarkdownWidgetComponent extends PageComponent implements OnInit {

  settings: MarkdownWidgetSettings;
  markdownTextFunction: MarkdownTextFunction;

  @HostBinding('class')
  markdownClass: string;

  @Input()
  ctx: WidgetContext;

  markdownText: string;


  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.markdownWidget = this;
    this.settings = this.ctx.settings;
    this.markdownTextFunction = this.settings.useMarkdownTextFunction ? parseFunction(this.settings.markdownTextFunction, ['data']) : null;
    this.markdownClass = 'markdown-widget';
    const cssString = this.settings.markdownCss;
    if (isNotEmptyStr(cssString)) {
      const cssParser = new cssjs();
      cssParser.testMode = false;
      this.markdownClass += '-' + hashCode(cssString);
      cssParser.cssPreviewNamespace = this.markdownClass;
      cssParser.createStyleElement(this.markdownClass, cssString);
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
    const data = parseData(initialData);
    let markdownText = this.settings.useMarkdownTextFunction ?
      safeExecute(this.markdownTextFunction, [data]) : this.settings.markdownTextPattern;
    const allData = flatData(data);
    const replaceInfo = processPattern(markdownText, allData);
    markdownText = fillPattern(markdownText, replaceInfo, allData);
    if (this.markdownText !== markdownText) {
      this.markdownText = this.utils.customTranslation(markdownText, markdownText);
      this.cd.detectChanges();
    }
  }

  markdownClick($event: MouseEvent) {
    this.ctx.actionsApi.elementClick($event);
  }

}
