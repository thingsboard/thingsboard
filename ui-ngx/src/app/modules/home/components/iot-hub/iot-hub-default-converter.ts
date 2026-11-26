// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { firstValueFrom } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { Converter, ConverterConfigV2, ConverterType, getTargetField, getTargetTemplateUrl } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';

const DEFAULT_CONVERTERS_V2_CONFIGURATION_URL = '/assets/converters/default-converters-v2-configuration.json';

/**
 * The uplink converter ThingsBoard creates for a new integration of this type, as a device package step with
 * "default": true installs it. Null when ThingsBoard has no default converter settings for the type.
 */
export const loadDefaultUplinkConverter = async (resourcesService: ResourcesService, integrationType: IntegrationType,
                                                 scriptLang: ScriptLanguage, name: string): Promise<Partial<Converter> | null> => {
  const configurations = await firstValueFrom(
    resourcesService.loadJsonResource<Record<string, ConverterConfigV2>>(DEFAULT_CONVERTERS_V2_CONFIGURATION_URL));
  const configuration = configurations?.[integrationType];
  if (!configuration) {
    return null;
  }
  const script = await firstValueFrom(resourcesService.loadJsonResource<string>(
    getTargetTemplateUrl(ConverterType.UPLINK, scriptLang, integrationType, 2)));
  return {
    name,
    type: ConverterType.UPLINK,
    integrationType,
    converterVersion: 2,
    edgeTemplate: false,
    // As the device packages had it: the debug events help to find out why a LoRaWAN device sends nothing
    debugSettings: { failuresEnabled: true, allEnabled: true, allEnabledUntil: 0 },
    configuration: {
      ...configuration,
      scriptLang,
      decoder: null,
      tbelDecoder: null,
      encoder: null,
      tbelEncoder: null,
      [getTargetField(ConverterType.UPLINK, scriptLang)]: script
    },
    additionalInfo: { description: '' }
  };
};
