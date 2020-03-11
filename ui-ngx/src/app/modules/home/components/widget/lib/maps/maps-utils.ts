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
        let angle = -Math.atan2(endPoint.latitude - startPoint.longitude, endPoint.longitude - startPoint.latitude);
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
