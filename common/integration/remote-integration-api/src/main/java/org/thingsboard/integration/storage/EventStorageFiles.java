// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import lombok.Data;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class EventStorageFiles {
    private final File stateFile;
    private final CopyOnWriteArrayList<File> dataFiles;
}
