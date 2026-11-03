// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ScadaSymbolObjectSettings } from '@home/components/widget/lib/scada/scada-symbol.models';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';

export interface ScadaSymbolWidgetSettings {
  scadaSymbolUrl?: string;
  scadaSymbolContent?: string;
  simulated?: boolean;
  scadaSymbolObjectSettings: ScadaSymbolObjectSettings;
  background: BackgroundSettings;
  padding: string;
}

export const scadaSymbolWidgetDefaultSettings: ScadaSymbolWidgetSettings = {
  scadaSymbolObjectSettings: {
    behavior: {},
    properties: {}
  },
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};
