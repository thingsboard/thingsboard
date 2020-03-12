///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import L from 'leaflet';
import { interpolateOnPointSegment } from 'leaflet-geometryutil';
import _ from 'lodash';

export function createTooltip(target, settings, targetArgs?) {
    const popup = L.popup();
    popup.setContent('');
    target.bindPopup(popup, { autoClose: settings.autocloseTooltip, closeOnClick: false });
    if (settings.displayTooltipAction == 'hover') {
        target.off('click');
        target.on('mouseover', function () {
            this.openPopup();
        });
        target.on('mouseout', function () {
            this.closePopup();
        });
    }
    return {
        markerArgs: targetArgs,
        popup: popup,
        locationSettings: settings,
        dsIndex: settings.dsIndex
    };
}


export function interpolateArray(originData, interpolatedIntervals) {

    const getRatio = (firsMoment, secondMoment, intermediateMoment) => {
        return (intermediateMoment - firsMoment) / (secondMoment - firsMoment);
    };

    function findAngle(startPoint, endPoint) {
        let angle = -Math.atan2(endPoint.latitude - startPoint.latitude, endPoint.longitude - startPoint.longitude);
        angle = angle * 180 / Math.PI;
        return parseInt(angle.toFixed(2));
    }

    const result = {};

    for (let i = 1, j = 0; i < originData.length, j < interpolatedIntervals.length;) {
        const currentTime = interpolatedIntervals[j];
        while (originData[i].time < currentTime) i++;
        const before = originData[i - 1];
        const after = originData[i];
        const interpolation = interpolateOnPointSegment(
            new L.Point(before.latitude, before.longitude),
            new L.Point(after.latitude, after.longitude),
            getRatio(before.time, after.time, currentTime));
        result[currentTime] = ({
            ...originData[i],
            rotationAngle: findAngle(before, after),
            latitude: interpolation.x,
            longitude: interpolation.y
        });
        j++;
    }

    return result;
};
