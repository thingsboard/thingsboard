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

import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableAccessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.impl.refl.collection.ExprValueAccessor;

import java.util.HashMap;
import java.util.Map;

public class TbMapCreator implements Accessor {
    private Accessor[] keys;
    private Accessor[] vals;
    private int size;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Map map = new HashMap<>(size * 2);
        for (int i = size - 1; i != -1; i--) {
            //noinspection unchecked
            map.put(getKey(i, ctx, elCtx, variableFactory), vals[i].getValue(ctx, elCtx, variableFactory));
        }
        return map;
    }

    private Object getKey(int index, Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Accessor keyAccessor = keys[index];
        if (keyAccessor instanceof ExprValueAccessor) {
            ExecutableStatement executableStatement = ((ExprValueAccessor) keyAccessor).stmt;
            if (executableStatement instanceof ExecutableAccessor) {
                return ((ExecutableAccessor) executableStatement).getNode().getName();
            }
        }
        return keys[index].getValue(ctx, elCtx, variableFactory);
    }

    public TbMapCreator(Accessor[] keys, Accessor[] vals) {
        this.size = (this.keys = keys).length;
        this.vals = vals;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }

    public Class getKnownEgressType() {
        return Map.class;
    }
}
