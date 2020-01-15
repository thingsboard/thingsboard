package org.thingsboard.server.dao.sqlts;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

@Data
@AllArgsConstructor
public class EntityContainer<T extends AbstractTsKvEntity> {

        private T entity;
        private String partitionDate;

}
