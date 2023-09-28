/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import static org.thingsboard.server.dao.util.mapping.AbstractArrayType.SQL_ARRAY_TYPE;

public abstract class AbstractArrayTypeDescriptor<T>
        extends AbstractTypeDescriptor<T> implements DynamicParameterizedType {

    private Class<T> arrayObjectClass;

    private String sqlArrayType;

    public AbstractArrayTypeDescriptor(Class<T> arrayObjectClass) {
        this(arrayObjectClass, (MutabilityPlan<T>) new MutableMutabilityPlan<Object>() {
            @Override
            protected T deepCopyNotNull(Object value) {
                return ArrayUtil.deepCopy(value);
            }
        });
    }

    protected AbstractArrayTypeDescriptor(Class<T> arrayObjectClass, MutabilityPlan<T> mutableMutabilityPlan) {
        super(arrayObjectClass, mutableMutabilityPlan);
        this.arrayObjectClass = arrayObjectClass;
    }

    public Class<T> getArrayObjectClass() {
        return arrayObjectClass;
    }

    public void setArrayObjectClass(Class<T> arrayObjectClass) {
        this.arrayObjectClass = arrayObjectClass;
    }

    @Override
    public void setParameterValues(Properties parameters) {
        if (parameters.containsKey(PARAMETER_TYPE)) {
            arrayObjectClass = ((ParameterType) parameters.get(PARAMETER_TYPE)).getReturnedClass();
        }
        sqlArrayType = parameters.getProperty(SQL_ARRAY_TYPE);
    }

    @Override
    public boolean areEqual(T one, T another) {
        if (one == another) {
            return true;
        }
        if (one == null || another == null) {
            return false;
        }
        return ArrayUtil.isEquals(one, another);
    }

    @Override
    public String toString(T value) {
        return Arrays.deepToString(ArrayUtil.wrapArray(value));
    }

    @Override
    public T fromString(String string) {
        return ArrayUtil.fromString(string, arrayObjectClass);
    }

    @Override
    public String extractLoggableRepresentation(T value) {
        return (value == null) ? "null" : toString(value);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
        return (X) ArrayUtil.wrapArray(value);
    }

    @Override
    public <X> T wrap(X value, WrapperOptions options) {
        if (value instanceof Array) {
            Array array = (Array) value;
            try {
                return ArrayUtil.unwrapArray((Object[]) array.getArray(), arrayObjectClass);
            } catch (SQLException e) {
                throw new HibernateException(
                        new IllegalArgumentException(e)
                );
            }
        }
        return (T) value;
    }

    protected String getSqlArrayType() {
        return sqlArrayType;
    }
}
