/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.util.mapping;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public abstract class AbstractJsonSqlTypeDescriptor
        implements SqlTypeDescriptor {

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public boolean canBeRemapped() {
        return true;
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(
            final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicExtractor<X>(javaTypeDescriptor, this) {
            @Override
            protected X doExtract(
                    ResultSet rs,
                    String name,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        rs.getObject(name), options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    int index,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getObject(index), options
                );
            }

            @Override
            protected X doExtract(
                    CallableStatement statement,
                    String name,
                    WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap(
                        statement.getObject(name), options
                );
            }
        };
    }

}