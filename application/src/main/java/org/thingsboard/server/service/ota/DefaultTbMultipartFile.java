/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ota;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.dao.ota.TbMultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultTbMultipartFile implements TbMultipartFile {

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
