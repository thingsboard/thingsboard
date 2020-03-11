export interface MapWidgetInterface {
    resize(),
    update(),
    onInit(),
    onDataUpdated();
    onResize();
    onDestroy();
}

export interface MapWidgetStaticInterface {
    settingsSchema(mapProvider?, drawRoutes?): Object;
    getProvidersSchema():Object
    dataKeySettingsSchema(): Object;
    actionSources(): Object;
}
