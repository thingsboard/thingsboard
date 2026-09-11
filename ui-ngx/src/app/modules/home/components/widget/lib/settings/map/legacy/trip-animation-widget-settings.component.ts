// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CircleSettings,
  defaultCircleSettings,
  defaultMapProviderSettings,
  defaultPolygonSettings,
  defaultTripAnimationCommonSettings,
  defaultTripAnimationMarkersSettings,
  defaultTripAnimationPathSettings,
  defaultTripAnimationPointSettings,
  defaultTripAnimationSettings,
  MapProviderSettings,
  PointsSettings,
  PolygonSettings,
  PolylineSettings,
  TripAnimationCommonSettings,
  TripAnimationMarkerSettings
} from 'src/app/modules/home/components/widget/lib/maps-legacy/map-models';
import { extractType } from '@core/utils';

@Component({
    selector: 'tb-trip-animation-widget-settings',
    templateUrl: './trip-animation-widget-settings.component.html',
    styleUrls: ['./../../widget-settings.scss'],
    standalone: false
})
export class TripAnimationWidgetSettingsComponent extends WidgetSettingsComponent {

  tripAnimationWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.tripAnimationWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return defaultTripAnimationSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.tripAnimationWidgetSettingsForm = this.fb.group({
      mapProviderSettings: [settings.mapProviderSettings, []],
      commonMapSettings: [settings.commonMapSettings, []],
      markersSettings: [settings.markersSettings, []],
      pathSettings: [settings.pathSettings, []],
      pointSettings: [settings.pointSettings, []],
      polygonSettings: [settings.polygonSettings, []],
      circleSettings: [settings.circleSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const mapProviderSettings = extractType<MapProviderSettings>(settings, Object.keys(defaultMapProviderSettings) as (keyof MapProviderSettings)[]);
    const commonMapSettings = extractType<TripAnimationCommonSettings>(settings, Object.keys(defaultTripAnimationCommonSettings) as (keyof TripAnimationCommonSettings)[]);
    const markersSettings = extractType<TripAnimationMarkerSettings>(settings, Object.keys(defaultTripAnimationMarkersSettings) as (keyof TripAnimationMarkerSettings)[]);
    const pathSettings = extractType<PolylineSettings>(settings, Object.keys(defaultTripAnimationPathSettings) as (keyof PolylineSettings)[]);
    const pointSettings = extractType<PointsSettings>(settings, Object.keys(defaultTripAnimationPointSettings) as (keyof PointsSettings)[]);
    const polygonSettings = extractType<PolygonSettings>(settings, Object.keys(defaultPolygonSettings) as (keyof PolygonSettings)[]);
    const circleSettings = extractType<CircleSettings>(settings, Object.keys(defaultCircleSettings) as (keyof CircleSettings)[]);
    return {
      mapProviderSettings,
      commonMapSettings,
      markersSettings,
      pathSettings,
      pointSettings,
      polygonSettings,
      circleSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.mapProviderSettings,
      ...settings.commonMapSettings,
      ...settings.markersSettings,
      ...settings.pathSettings,
      ...settings.pointSettings,
      ...settings.polygonSettings,
      ...settings.circleSettings,
    };
  }
}
