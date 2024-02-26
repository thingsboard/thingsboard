/*
Author Ahmet Ertuğrul KAYA
*/
package org.thingsboard.server.update.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.event.Event;

public interface EventService {

    ListenableFuture<Void> saveAsync(Event event);

    void migrateEvents();

}
