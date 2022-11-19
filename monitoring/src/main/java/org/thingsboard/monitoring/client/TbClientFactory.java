package org.thingsboard.monitoring.client;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

@Component
public class TbClientFactory {

    @Lookup
    public TbRestClient createClient() {
        return null;
    }

}
