package com.networknt.rule.generic.token.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public abstract class SharedVariableRead extends SharedVariableSchema {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVariableRead.class);

    public abstract void writeSchemaFromSharedVariables(final SharedVariableSchema sharedVariableSchema);

    protected static void updateMapFromSharedVariables(final Map<String, String> map, final SharedVariableSchema sharedVariableSchema) {
        if (map == null)
            return;

        final var updateMap = new HashMap<String, String>();
        for (final var entry : map.entrySet()) {
            final var key = entry.getKey();
            final var value = injectSharedVariableValue(sharedVariableSchema, entry.getValue());
            updateMap.put(key, value);
        }
        map.putAll(updateMap);
    }

    /**
     * Inject sharedVariable values into values that use !ref.
     *
     * @param sharedVariableSchema
     * @param variableString
     * @return
     */
    public static String injectSharedVariableValue(final SharedVariableSchema sharedVariableSchema, final String variableString) {

        BeanInfo beaninfo;
        try {
            beaninfo = Introspector.getBeanInfo(SharedVariableSchema.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        final var matcher = VARIABLE_PATTERN.matcher(variableString);
        final var stringBuilder = new StringBuilder();

        while (matcher.find()) {
            final var foundVariableName = matcher.group(1);
            final var splitVariable = foundVariableName.split("\\.");
            final String value;
            value = readFromSharedVariable(sharedVariableSchema, beaninfo, splitVariable[1]);

            if (value.contains("\\!ref"))
                matcher.appendReplacement(stringBuilder, value);

            else matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(value));

        }
        return matcher.appendTail(stringBuilder).toString();
    }

    /**
     * Grab the associated sharedVariable field value using BeanInfo.
     *
     * @param sharedVariableSchema
     * @param beanInfo
     * @param sharedVariableName
     * @return
     */
    private static String readFromSharedVariable(final SharedVariableSchema sharedVariableSchema, final BeanInfo beanInfo, final String sharedVariableName) {
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        Method getterMethod;

        for (PropertyDescriptor pd : pds) {

            if (pd.getName().equalsIgnoreCase(sharedVariableName)) {
                getterMethod = pd.getReadMethod();

                if (getterMethod != null) {
                    LOG.trace("Found getter method for '{}'", sharedVariableName);
                    try {
                        final var returnType = getterMethod.getReturnType();

                        if (returnType == String.class) {

                            LOG.trace("Getter method return type for '{}' is of string type.", sharedVariableName);

                            return (String) getterMethod.invoke(sharedVariableSchema);

                        } else if (returnType.getName().equalsIgnoreCase("[C")) {

                            LOG.trace("Getter method return type for '{}' is of char array type.", sharedVariableName);

                            return String.valueOf((char[]) getterMethod.invoke(sharedVariableSchema));

                        } else if (returnType.getName().equalsIgnoreCase("long")) {

                            LOG.trace("Getter method return type for '{}' is of long type.", sharedVariableName);

                            return String.valueOf(getterMethod.invoke(sharedVariableSchema));
                        }

                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unknown variable found: " + sharedVariableName);
    }


}
