// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import LeafletMap from '@home/components/widget/lib/maps-legacy/leaflet-map';

export interface MapWidgetInterface {
    map?: LeafletMap;
    resize(): void;
    update(): void;
    destroy(): void;
}

export interface MapWidgetStaticInterface {
    actionSources(): object;
}
