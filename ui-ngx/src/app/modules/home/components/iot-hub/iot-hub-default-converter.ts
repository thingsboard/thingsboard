// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { firstValueFrom } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { Converter, ConverterConfigV2, ConverterType, getTargetField, getTargetTemplateUrl } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { isEqual } from '@core/utils';

const DEFAULT_CONVERTERS_V2_CONFIGURATION_URL = '/assets/converters/default-converters-v2-configuration.json';

// The settings of a default converter that decide what it does: the script is compared on its own
const V2_CONFIGURATION_KEYS: (keyof ConverterConfigV2)[] =
  ['type', 'name', 'profile', 'label', 'customer', 'group', 'attributes', 'telemetry', 'updateOnlyKeys'];

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

/** True when the converter does something else than the default one: another script or other settings. */
export const differsFromDefaultConverter = (converter: Converter, defaultConverter: Partial<Converter>): boolean => {
  const field = getTargetField(ConverterType.UPLINK, converter.configuration.scriptLang);
  const script = (value: string) => (value ?? '').trimEnd();
  return script(converter.configuration[field]) !== script(defaultConverter.configuration[field]) ||
    V2_CONFIGURATION_KEYS.some(key => !isEqual(converter.configuration[key] ?? null, defaultConverter.configuration[key] ?? null));
};
