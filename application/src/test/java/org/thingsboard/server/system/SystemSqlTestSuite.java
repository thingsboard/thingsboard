/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.system;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.cassandra.cql3.functions.ThreadAwareSecurityManager;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.internal.bytebuddy.HibernateMethodLookupDispatcher;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.thingsboard.server.dao.CustomSqlUnit;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

/**
 * Created by Valerii Sosliuk on 6/27/2017.
 */
@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.system.sql.*SqlTest"})
public class SystemSqlTestSuite {

    static {
        //ThreadAwareSecurityManager.install();
        SecurityManager appsm = System.getSecurityManager();
        System.out.println("SECURITY MANAGER = " + appsm);
        if (appsm != null) {
            System.out.println("SECURITY MANAGER CLASS = " + appsm.getClass());
        }

        AsmVisitorWrapper.ForDeclaredMethods getDeclaredMethodMemberSubstitution;
        AsmVisitorWrapper.ForDeclaredMethods getMethodMemberSubstitution;

        //if ( System.getSecurityManager() != null ) {
            getDeclaredMethodMemberSubstitution = getDeclaredMethodMemberSubstitution();
            getMethodMemberSubstitution = getMethodMemberSubstitution();
        //}
        //else {
        //    getDeclaredMethodMemberSubstitution = null;
        //    getMethodMemberSubstitution = null;
        //}

        System.out.println("getDeclaredMethodMemberSubstitution = " + getDeclaredMethodMemberSubstitution);
        System.out.println("getMethodMemberSubstitution = " + getMethodMemberSubstitution);
    }

    private static class GetDeclaredMethodAction implements PrivilegedAction<Method> {
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] parameterTypes;

        private GetDeclaredMethodAction(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public Method run() {
            try {
                Method method = clazz.getDeclaredMethod( methodName, parameterTypes );

                return method;
            }
            catch (NoSuchMethodException e) {
                throw new HibernateException( "Unable to prepare getDeclaredMethod()/getMethod() substitution", e );
            }
        }
    }


    private static AsmVisitorWrapper.ForDeclaredMethods getDeclaredMethodMemberSubstitution() {
        // this should only be called if the security manager is enabled, thus the privileged calls
        return MemberSubstitution.relaxed()
                .method( ElementMatchers.is( AccessController.doPrivileged( new SystemSqlTestSuite.GetDeclaredMethodAction( Class.class,
                        "getDeclaredMethod", String.class, Class[].class ) ) ) )
                .replaceWith(
                        AccessController.doPrivileged( new SystemSqlTestSuite.GetDeclaredMethodAction( HibernateMethodLookupDispatcher.class,
                                "getDeclaredMethod", Class.class, String.class, Class[].class ) ) )
                .on( ElementMatchers.isTypeInitializer() );
    }

    private static AsmVisitorWrapper.ForDeclaredMethods getMethodMemberSubstitution() {
        // this should only be called if the security manager is enabled, thus the privileged calls
        return MemberSubstitution.relaxed()
                .method( ElementMatchers.is( AccessController.doPrivileged( new SystemSqlTestSuite.GetDeclaredMethodAction( Class.class,
                        "getMethod", String.class, Class[].class ) ) ) )
                .replaceWith(
                        AccessController.doPrivileged( new SystemSqlTestSuite.GetDeclaredMethodAction( HibernateMethodLookupDispatcher.class,
                                "getMethod", Class.class, String.class, Class[].class ) ) )
                .on( ElementMatchers.isTypeInitializer() );
    }

    @ClassRule
    public static CustomSqlUnit sqlUnit = new CustomSqlUnit(
            Arrays.asList("sql/schema-ts.sql", "sql/schema-entities.sql", "sql/system-data.sql"),
            "sql/drop-all-tables.sql",
            "sql-test.properties");

}
