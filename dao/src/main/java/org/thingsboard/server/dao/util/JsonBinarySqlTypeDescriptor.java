/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.*;

/**
 * Created by Valerii Sosliuk on 5/12/2017.
 */
public class JsonBinarySqlTypeDescriptor
        implements SqlTypeDescriptor {

    public static final JsonBinarySqlTypeDescriptor INSTANCE =
            new JsonBinarySqlTypeDescriptor();

    @Override
    public int getSqlType() {
        return Types.OTHER;
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

    @Override
    public <X> ValueBinder<X> getBinder(
            final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(
                    PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions options) throws SQLException {
                st.setObject(index,
                        javaTypeDescriptor.unwrap(
                                value, JsonNode.class, options), getSqlType()
                );
            }

            @Override
            protected void doBind(
                    CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions options)
                    throws SQLException {
                st.setObject(name,
                        javaTypeDescriptor.unwrap(
                                value, JsonNode.class, options), getSqlType()
                );
            }
        };
    }
}