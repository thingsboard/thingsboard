// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { DataKey, Widget, widgetType } from '@shared/models/widget.models';
import { Observable } from 'rxjs';

export interface MapSettingsContext {
  functionsOnly: boolean;
  aliasController: IAliasController;
  callbacks: WidgetConfigCallbacks;
  widget: Widget;
  editKey: (key: DataKey, deviceId: string, entityAliasId: string, WidgetType?: widgetType) => Observable<DataKey>;
  generateDataKey: (key: DataKey) => DataKey;
}
