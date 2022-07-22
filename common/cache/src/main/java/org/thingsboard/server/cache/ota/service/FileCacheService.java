package org.thingsboard.server.cache.ota.service;

import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Blob;

public interface FileCacheService {
    void deleteFile(File file);
    InputStream getOtaDataStream(OtaPackageId otaPackageId) throws FileNotFoundException ;
    InputStream loadOtaData(OtaPackageId otaPackageId, Blob data);
    File loadToFile(OtaPackageId otaPackageId, Blob data);
    File saveDataTemporaryFile(InputStream inputStream);
    File getOtaDataFile(OtaPackageId otaPackageId) throws FileNotFoundException;
}
