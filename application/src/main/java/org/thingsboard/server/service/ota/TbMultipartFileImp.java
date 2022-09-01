package org.thingsboard.server.service.ota;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.dao.ota.TbMultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RequiredArgsConstructor
public class TbMultipartFileImp implements TbMultipartFile {

    @NotNull
    private final MultipartFile file;

    @Override
    public Optional<InputStream> getInputStream() {
        try {
            return Optional.of(file.getInputStream());
        } catch (IOException e) {
            return Optional.empty();
        }

    }

    @Override
    public String getFileName() {
        return file.getName();
    }

    @Override
    public long getFileSize() {
        return file.getSize();
    }

    @Override
    public String getContentType() {
        return file.getContentType();
    }
}
