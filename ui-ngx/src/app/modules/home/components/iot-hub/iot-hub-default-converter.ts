// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { firstValueFrom } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { Converter, ConverterConfigV2, ConverterType, getTargetField, getTargetTemplateUrl } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { isEqual } from '@core/utils';
import { DeviceInstalledItemDescriptor, DeviceUpdateRequest, IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

/** An installed device package's default converter that differs from ThingsBoard's default converter now. */
export interface DefaultConverterUpdate {
  installedItem: IotHubInstalledItem;
  // The key of the converter's step in the package's install state
  stateKey: string;
  converter: Converter;
  defaultConverter: Partial<Converter>;
}

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

/** All installed device packages of the tenant. */
export const loadInstalledDevicePackages = async (iotHubApiService: IotHubApiService): Promise<IotHubInstalledItem[]> => {
  const items: IotHubInstalledItem[] = [];
  let pageLink = new PageLink(100);
  let page: PageData<IotHubInstalledItem>;
  do {
    page = await firstValueFrom(iotHubApiService.getInstalledItems(pageLink, 'DEVICE', undefined, { ignoreLoading: true }));
    items.push(...page.data);
    pageLink = pageLink.nextPageLink();
  } while (page.hasNext);
  return items;
};

/**
 * The converters device packages installed themselves as their default converter, not ones they took as they were, of
 * all installed device packages: only such a converter is ThingsBoard's default one, which a new ThingsBoard version
 * may change. The installed items table and the update dialog both decide by it.
 */
export const loadOwnDefaultConverterIds = async (iotHubApiService: IotHubApiService): Promise<Set<string>> => {
  const ids = new Set<string>();
  for (const item of await loadInstalledDevicePackages(iotHubApiService)) {
    for (const state of Object.values((item.descriptor as DeviceInstalledItemDescriptor)?.installState ?? {})) {
      if (state?.defaultConverter && state.entityOutput?.id && state.resolution !== 'use-existing') {
        ids.add(state.entityOutput.id);
      }
    }
  }
  return ids;
};

/**
 * The update of the installed version itself that brings its default converter to ThingsBoard's default one. It sends only
 * the converter step: the server keeps the other steps, the forms and the integration's metadata as they are.
 * expectedVersion is the converter's version the user reviewed.
 */
export const defaultConverterUpdateRequest = (update: DefaultConverterUpdate, expectedVersion?: number): DeviceUpdateRequest => {
  const descriptor = update.installedItem.descriptor as DeviceInstalledItemDescriptor;
  return {
    versionId: update.installedItem.itemVersionId,
    fromVersionId: update.installedItem.itemVersionId,
    selectedInstallMethod: descriptor.selectedInstallMethod,
    installState: {},
    steps: [{
      name: update.converter.name,
      id: update.stateKey,
      type: 'UPLINK_CONVERTER',
      action: 'OVERWRITE',
      entityId: update.converter.id.id,
      expectedVersion,
      // The user confirmed it in the dialog, which names the other packages it affects
      confirmed: true,
      resolution: descriptor.installState?.[update.stateKey]?.resolution ?? 'created',
      defaultConverter: true,
      entity: update.defaultConverter
    }]
  };
};
