// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.util.Objects;

@ToString
@AllArgsConstructor
class EventStorageReaderPointer {

    @Getter @Setter
    private File file;
    @Getter @Setter
    private int line;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStorageReaderPointer that = (EventStorageReaderPointer) o;
        return line == that.line &&
                Objects.equals(file.getName(), that.file.getName());
    }

    public EventStorageReaderPointer copy(){
        return new EventStorageReaderPointer(file, line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file.getName(), line);
    }
}
