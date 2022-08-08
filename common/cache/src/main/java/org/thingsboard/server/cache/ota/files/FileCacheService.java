package org.thingsboard.server.cache.ota.files;

import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

public interface FileCacheService {
    File loadToFile(OtaPackageId otaPackageId, InputStream data);
    File saveDataTemporaryFile(InputStream inputStream);
    Optional<File> getOtaDataFile(OtaPackageId otaPackageId) throws FileNotFoundException;
}
