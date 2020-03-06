export interface MapWidgetInterface {
    resize(),
    update(),
    onInit(),
    onDataUpdated();
    onResize();
    onDestroy();
}

export interface MapWidgetStaticInterface {
    settingsSchema(mapProvider, drawRoutes): Object;
    dataKeySettingsSchema(): Object;
    actionSources(): Object;
}
