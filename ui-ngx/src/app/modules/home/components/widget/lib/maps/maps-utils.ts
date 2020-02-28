import L from 'leaflet';

export function createTooltip(target, dsIndex, settings, targetArgs?) {
    var popup = L.popup();
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
        dsIndex: dsIndex
    };
}