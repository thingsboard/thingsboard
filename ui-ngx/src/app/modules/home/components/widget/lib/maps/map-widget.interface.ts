export interface MapWidgetInterface {
    resize(),
    onInit(),
    onDataUpdated();
    onResize();
    getSettingsSchema(): Object;
    onDestroy();
}

export interface MapWidgetStaticInterface {
    settingsSchema(mapProvider, drawRoutes): Object;
    dataKeySettingsSchema(): Object;
    actionSources(): Object;
}
