package org.thingsboard.server.dao.sql.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaBaseComponentDescriptorDao extends JpaAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor>
    implements ComponentDescriptorDao {

    @Autowired
    private ComponentDescriptorRepository componentDescriptorRepository;

    @Override
    protected Class<ComponentDescriptorEntity> getEntityClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME;
    }

    @Override
    protected CrudRepository<ComponentDescriptorEntity, UUID> getCrudRepository() {
        return componentDescriptorRepository;
    }

    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptor component) {
        boolean exists = componentDescriptorRepository.findOne(component.getId().getId()) != null;
        if (exists) {
            return Optional.empty();
        }
        return Optional.of(save(component));
    }

    @Override
    public ComponentDescriptor findById(ComponentDescriptorId componentId) {
        return findById(componentId.getId());
    }

    @Override
    public ComponentDescriptor findByClazz(String clazz) {
        return DaoUtil.getData(componentDescriptorRepository.findByClazz(clazz));
    }

    @Override
    public List<ComponentDescriptor> findByTypeAndPageLink(ComponentType type, TextPageLink pageLink) {
        if (pageLink.getIdOffset() == null) {
            return DaoUtil.convertDataList(componentDescriptorRepository.findByTypeFirstPage(pageLink.getLimit(),
                    type.ordinal(), pageLink.getTextSearch()));
        } else {
            return DaoUtil.convertDataList(componentDescriptorRepository.findByTypeNextPage(pageLink.getLimit(),
                    type.ordinal(), pageLink.getTextSearch(), pageLink.getIdOffset()));
        }
    }

    @Override
    public List<ComponentDescriptor> findByScopeAndTypeAndPageLink(ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        if (pageLink.getIdOffset() == null) {
            return DaoUtil.convertDataList(componentDescriptorRepository.findByScopeAndTypeFirstPage(pageLink.getLimit(),
                    type.ordinal(), scope.ordinal(), pageLink.getTextSearch()));
        } else {
            return DaoUtil.convertDataList(componentDescriptorRepository.findByScopeAndTypeNextPage(pageLink.getLimit(),
                    type.ordinal(), scope.ordinal(), pageLink.getTextSearch(), pageLink.getIdOffset()));
        }
    }

    @Override
    public void deleteById(ComponentDescriptorId componentId) {
        removeById(componentId.getId());
    }

    @Override
    public void deleteByClazz(String clazz) {
        componentDescriptorRepository.deleteByClazz(clazz);
    }
}
