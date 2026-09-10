// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.function.Functions;

import java.util.ArrayList;
import java.util.List;

public class ExpressionFunctionsUtil {

    public static final List<Function> userDefinedFunctions = new ArrayList<>();

    static {
        userDefinedFunctions.add(
                new Function("ln") {
                    @Override
                    public double apply(double... args) {
                        return Math.log(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(
                new Function("lg") {
                    @Override
                    public double apply(double... args) {
                        return Math.log10(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(
                new Function("logab", 2) {
                    @Override
                    public double apply(double... args) {
                        return Math.log(args[1]) / Math.log(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(Functions.getBuiltinFunction("sin"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cos"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("tan"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cot"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log2"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log10"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log1p"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("abs"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("acos"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("asin"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("atan"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cbrt"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("floor"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("sinh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("sqrt"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("tanh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cosh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("ceil"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("pow"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("exp"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("expm1"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("signum"));
    }

}
