package org.thingsboard.server.service.resource;

import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;

public interface TbImageService {

    TbResource save(TbResourceInfo imageInfo, MultipartFile imageFile, User user);

    void delete(TbResourceInfo imageInfo, User user);

}
