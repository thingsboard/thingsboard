package org.thingsboard.server.dao.edq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SortPair {

    @Getter
    final String key;
    @Getter
    final DeviceRepoData data;

}
