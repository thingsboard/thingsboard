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
    return alliases.map((alliasArray, dsIndex) =>
        alliasArray[0].data.map((el, i) => {
            const obj = {
                aliasName: alliasArray[0]?.datasource?.aliasName,
                $datasource: alliasArray[0]?.datasource,
                dsIndex: dsIndex
            };
            alliasArray.forEach(el => {
                obj[el?.dataKey?.label] = el?.data[i][1];
                obj[el?.dataKey?.label + '|ts'] = el?.data[0][0];
            });
            return obj;
        })
    );
}

export function parseData(input: any[]): any[] {
    return _(input).groupBy(el => el?.datasource?.aliasName).values().value().map((alliasArray, i) => {
        const obj = {
            aliasName: alliasArray[0]?.datasource?.aliasName,
            $datasource: alliasArray[0]?.datasource,
            dsIndex: i
        };
        alliasArray.forEach(el => {
            obj[el?.dataKey?.label] = el?.data[0][1];
            obj[el?.dataKey?.label + '|ts'] = el?.data[0][0];
        });
        return obj;
    });
}

export function safeExecute(func: Function, params = []) {  
    let res = null;
    if (func && typeof (func) == "function") {
        try {
            res = func(...params);
        }
        catch (err) {
            console.error(err);
            res = null;
        }
    }
    return res;
}

export function parseFunction(source: string, params: string[] = []): Function {
    let res = null;
    if (source?.length) {
        try {
            res = new Function(...params, source);
        }
        catch (err) {
            console.error(err);
            res = null;
        }
    }
    return res;
}