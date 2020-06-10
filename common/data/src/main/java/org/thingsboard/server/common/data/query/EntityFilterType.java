package org.thingsboard.server.common.data.query;

public enum EntityFilterType {
    SINGLE_ENTITY("singleEntity"),
    ENTITY_LIST("entityList"),
    ENTITY_NAME("entityName");
//    stateEntity = 'stateEntity',
//    assetType = 'assetType',
//    deviceType = 'deviceType',
//    entityViewType = 'entityViewType',
//    relationsQuery = 'relationsQuery',
//    assetSearchQuery = 'assetSearchQuery',
//    deviceSearchQuery = 'deviceSearchQuery',
//    entityViewSearchQuery = 'entityViewSearchQuery'

    private final String label;

    EntityFilterType(String label) {
        this.label = label;
    }
}
