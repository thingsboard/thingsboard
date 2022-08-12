package org.thingsboard.server.dao.ota;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface TbMultipartFile {
    Optional<InputStream> getInputStream();

    String getFileName();

    long getFileSize();

    String getContentType();
}
