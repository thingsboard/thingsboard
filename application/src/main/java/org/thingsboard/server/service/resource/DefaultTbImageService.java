package org.thingsboard.server.service.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbImageService implements TbImageService {

    private final ImageService imageService;

    @Override
    public TbResource save(TbResourceInfo imageInfo, MultipartFile imageFile, User user) {
        return null;
    }

    @Override
    public void delete(TbResourceInfo imageInfo, User user) {

    }

}
