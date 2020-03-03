import L from 'leaflet';
import _ from 'lodash';

export function createTooltip(target, settings, targetArgs?) {
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
        dsIndex: settings.dsIndex
    };
}

export function parseArray(input: any[]): any[] {
    let alliases: any = _(input).groupBy(el => el?.datasource?.aliasName).values().value();
    return alliases.map(alliasArray =>
        alliasArray[0].data.map((el, i) => {            
            const obj = { aliasName: alliasArray[0]?.datasource?.aliasName };
            alliasArray.forEach(el => {
                obj[el?.dataKey?.label] = el?.data[i][1]
            });
            return obj;
        })
    ).flat();
}

export function parseData(input: any[]): any[] {
    return _(input).groupBy(el => el?.datasource?.aliasName).values().value().map(alliasArray => {
        const obj = { aliasName: alliasArray[0]?.datasource?.aliasName };
        alliasArray.forEach(el => {
            obj[el?.dataKey?.label] = el?.data[0][1]
        });
        return obj;
    });
}