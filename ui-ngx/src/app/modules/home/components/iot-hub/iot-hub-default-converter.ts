// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { firstValueFrom } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { Converter, ConverterConfigV2, ConverterType, getTargetField, getTargetTemplateUrl } from '@shared/models/converter.models';
import { IntegrationType } from '@shared/models/integration.models';
import { ScriptLanguage } from '@shared/models/rule-node.models';
import { isEqual, objectHashCode } from '@core/utils';
import { DeviceInstalledItemDescriptor, DevicePackageRequest, IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
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
const differsFromDefaultConverter = (converter: Converter, defaultConverter: Partial<Converter>): boolean => {
  const field = getTargetField(ConverterType.UPLINK, converter.configuration.scriptLang);
  const script = (value: string) => (value ?? '').trimEnd();
  return script(converter.configuration[field]) !== script(defaultConverter.configuration[field]) ||
    V2_CONFIGURATION_KEYS.some(key => !isEqual(converter.configuration[key] ?? null, defaultConverter.configuration[key] ?? null));
};

/**
 * The script language and a hash of what differsFromDefaultConverter compares. A step that writes the default converter
 * records it in its install state, so that a later check tells ThingsBoard's change of its default converter from the
 * user's change of the converter.
 */
export const defaultConverterFingerprint = (defaultConverter: Partial<Converter>): string => {
  const configuration = defaultConverter.configuration;
  const field = getTargetField(ConverterType.UPLINK, configuration.scriptLang);
  return configuration.scriptLang + ':' + objectHashCode([(configuration[field] ?? '').trimEnd(),
    ...V2_CONFIGURATION_KEYS.map(key => configuration[key] ?? null)]);
};

/**
 * True when ThingsBoard's default converter changed since a package last wrote the converter. Packages that share the
 * converter each record only their own write, so it is current while any of their fingerprints matches ThingsBoard's
 * default converter now, in the language that fingerprint was taken in. A converter no package recorded a fingerprint
 * for is compared itself, and only while nobody changed it: modified comes from the package's check.
 */
export const defaultConverterChanged = async (resourcesService: ResourcesService, converter: Converter,
                                              fingerprints: Set<string> | undefined, modified?: boolean | null): Promise<boolean> => {
  if (fingerprints?.size) {
    for (const fingerprint of fingerprints) {
      const scriptLang = fingerprint.substring(0, fingerprint.indexOf(':')) as ScriptLanguage;
      const defaultConverter = scriptLang ?
        await loadDefaultUplinkConverter(resourcesService, converter.integrationType, scriptLang, converter.name) : null;
      if (defaultConverter && defaultConverterFingerprint(defaultConverter) === fingerprint) {
        return false;
      }
    }
    return true;
  }
  const defaultConverter = await loadDefaultUplinkConverter(resourcesService, converter.integrationType,
    converter.configuration.scriptLang, converter.name);
  return modified === false && !!defaultConverter && differsFromDefaultConverter(converter, defaultConverter);
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

/** What all installed device packages recorded about their default converters. The installed items table and the update dialog both decide by it. */
export interface DefaultConverterRecords {
  // The converters packages installed themselves as their default converter, not ones they took as they were: only such
  // a converter is ThingsBoard's default one, which a new ThingsBoard version may change
  ownIds: Set<string>;
  // By converter id: the defaultConverterFingerprint each package recorded when it wrote the converter
  fingerprints: Map<string, Set<string>>;
}

export const loadDefaultConverterRecords = async (iotHubApiService: IotHubApiService): Promise<DefaultConverterRecords> => {
  const records: DefaultConverterRecords = { ownIds: new Set(), fingerprints: new Map() };
  for (const item of await loadInstalledDevicePackages(iotHubApiService)) {
    for (const state of Object.values((item.descriptor as DeviceInstalledItemDescriptor)?.installState ?? {})) {
      const id: string = state?.defaultConverter ? state.entityOutput?.id : undefined;
      if (!id) {
        continue;
      }
      if (state.resolution !== 'use-existing') {
        records.ownIds.add(id);
      }
      if (state.defaultConverterFingerprint) {
        if (!records.fingerprints.has(id)) {
          records.fingerprints.set(id, new Set());
        }
        records.fingerprints.get(id).add(state.defaultConverterFingerprint);
      }
    }
  }
  return records;
};

/**
 * The update of the installed version itself that brings its default converter to ThingsBoard's default one. It sends only
 * the converter step: the server keeps the other steps, the forms and the integration's metadata as they are.
 * expectedVersion is the converter's version the user reviewed.
 */
export const defaultConverterUpdateRequest = (update: DefaultConverterUpdate, expectedVersion?: number): DevicePackageRequest => {
  const descriptor = update.installedItem.descriptor as DeviceInstalledItemDescriptor;
  return {
    versionId: update.installedItem.itemVersionId,
    fromVersionId: update.installedItem.itemVersionId,
    selectedInstallMethod: descriptor.selectedInstallMethod,
    installState: {},
    steps: [{
      name: update.converter.name,
      // The server keys the step's state by its id: the state stays under the key it has
      id: update.stateKey,
      type: 'UPLINK_CONVERTER',
      action: 'OVERWRITE',
      entityId: update.converter.id.id,
      expectedVersion,
      // The user confirmed it in the dialog, which names the other packages it affects
      confirmed: true,
      resolution: descriptor.installState?.[update.stateKey]?.resolution ?? 'created',
      defaultConverter: true,
      defaultConverterFingerprint: defaultConverterFingerprint(update.defaultConverter),
      entity: update.defaultConverter
    }]
  };
};
