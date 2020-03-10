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

    const result = {};

    for (let i = 1, j = 0; i < originData.length, j < interpolatedIntervals.length;) {
        const currentTime = interpolatedIntervals[j];
        while (originData[i].time < currentTime) i++;
        const before = originData[i - 1];
        const after = originData[i];
        result[currentTime] = (interpolateOnPointSegment(
            new L.Point(before.latitude, before.longitude),
            new L.Point(after.latitude, after.longitude),
            getRatio(before.time, after.time, currentTime)));
        j++;
    }

    return result;
};
