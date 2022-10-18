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
package org.thingsboard.script.api.mvel;

import org.mvel2.ParserContext;
import org.mvel2.compiler.Accessor;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.impl.refl.ReflectiveAccessorOptimizer;
import org.mvel2.optimizers.impl.refl.collection.ArrayCreator;
import org.mvel2.optimizers.impl.refl.collection.ExprValueAccessor;
import org.mvel2.optimizers.impl.refl.collection.ListCreator;
import org.mvel2.optimizers.impl.refl.nodes.Union;

import java.util.List;
import java.util.Map;

import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.findClass;
import static org.mvel2.util.ParseTools.getBaseComponentType;
import static org.mvel2.util.ParseTools.getSubComponentType;
import static org.mvel2.util.ParseTools.repeatChar;

public class TbReflectiveAccessorOptimizer extends ReflectiveAccessorOptimizer {
    private Object ctx;
    private Class returnType;
    private VariableResolverFactory variableFactory;

    @Override
    public Accessor optimizeCollection(ParserContext pCtx, Object o, Class type, char[] property, int start, int offset,
                                       Object ctx, Object thisRef, VariableResolverFactory factory) {
        this.start = this.cursor = start;
        this.length = start + offset;
        this.returnType = type;
        this.ctx = ctx;
        this.variableFactory = factory;
        this.pCtx = pCtx;

        Accessor root = _getAccessor(o, returnType);

        if (property != null && length > start) {
            return new Union(pCtx, root, property, cursor, offset);
        } else {
            return root;
        }
    }

    private Accessor _getAccessor(Object o, Class type) {
        if (o instanceof List) {
            Accessor[] a = new Accessor[((List) o).size()];
            int i = 0;

            for (Object item : (List) o) {
                a[i++] = _getAccessor(item, type);
            }

            returnType = List.class;

            return new ListCreator(a);
        } else if (o instanceof Map) {
            Accessor[] k = new Accessor[((Map) o).size()];
            Accessor[] v = new Accessor[k.length];
            int i = 0;

            for (Object item : ((Map) o).keySet()) {
                k[i] = _getAccessor(item, type); // key
                v[i++] = _getAccessor(((Map) o).get(item), type); // value
            }

            returnType = Map.class;

            return new TbMapCreator(k, v);
        } else if (o instanceof Object[]) {
            Accessor[] a = new Accessor[((Object[]) o).length];
            int i = 0;
            int dim = 0;

            if (type != null) {
                String nm = type.getName();
                while (nm.charAt(dim) == '[') dim++;
            } else {
                type = Object[].class;
                dim = 1;
            }

            try {
                Class base = getBaseComponentType(type);
                Class cls = dim > 1 ? findClass(null, repeatChar('[', dim - 1) + "L" + base.getName() + ";", pCtx)
                        : type;

                for (Object item : (Object[]) o) {
                    expectType(pCtx, a[i++] = _getAccessor(item, cls), base, true);
                }

                return new ArrayCreator(a, getSubComponentType(type));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("this error should never throw:" + getBaseComponentType(type).getName(), e);
            }
        } else {
            if (returnType == null) returnType = Object.class;
            if (type.isArray()) {
                return new ExprValueAccessor((String) o, type, ctx, variableFactory, pCtx);
            } else {
                return new ExprValueAccessor((String) o, Object.class, ctx, variableFactory, pCtx);
            }
        }
    }
}
