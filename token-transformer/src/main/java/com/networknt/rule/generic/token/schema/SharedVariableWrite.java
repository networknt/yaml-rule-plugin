package com.networknt.rule.generic.token.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public abstract class SharedVariableWrite extends SharedVariableSchema {

    private static final Logger LOG = LoggerFactory.getLogger(SharedVariableWrite.class);

    public static void writeToSharedVariables(final SharedVariableSchema sharedVariableSchema, final Map<String, Object> sourceData, final List<SourceSchema.SourceDestinationDefinition> sourceDestinationMapping) {

        if (sourceDestinationMapping == null || sourceDestinationMapping.isEmpty() || sourceData == null || sourceData.isEmpty())
            return;

        BeanInfo beaninfo = null;
        try {
            beaninfo = Introspector.getBeanInfo(SharedVariableSchema.class);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        for (final var sourceEntry : sourceDestinationMapping) {

            if (sourceData.get(sourceEntry.getSource()) instanceof String) {
                final var sharedVariableValue = (String) sourceData.get(sourceEntry.getSource());
                final var matcher = VARIABLE_PATTERN.matcher(sourceEntry.getDestination());

                if (matcher.find()) {
                    final var variableName = matcher.group(1);

                    /* split prefix and suffix by the "." */
                    final var variableNameArray = variableName.split("\\.");

                    if (variableNameArray.length == 2)
                        pushToSharedVariable(sharedVariableSchema, beaninfo, variableNameArray[1], sharedVariableValue);

                    else throw new IllegalArgumentException("Invalid variable name provided: " + variableName);

                }
            }
        }
    }

    private static void pushToSharedVariable(final SharedVariableSchema sharedVariableSchema, final BeanInfo beanInfo, final String sharedVariableName, final String newSharedVariableValue) {

        LOG.trace("Attempting to set variable '{}' with the new value of '{}'", sharedVariableName, newSharedVariableValue);

        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        Method setterMethod;

        LOG.trace("Starting search for {} setter method via BeanInfo.", sharedVariableName);
        for (PropertyDescriptor descriptor : propertyDescriptors) {

            if (descriptor.getName().equalsIgnoreCase(sharedVariableName)) {
                setterMethod = descriptor.getWriteMethod(); // For Setter Method

                if (setterMethod != null) {

                    LOG.trace("Found setter method '{}'", setterMethod.getName());

                    try {
                        final var argTypes = setterMethod.getParameterTypes();

                        if (argTypes.length != 1) {
                            LOG.error("Setter method '{}' has {} parameters instead of 1", setterMethod.getName(), argTypes.length);
                            continue;
                        }

                        final var onlyArg = argTypes[0];

                        /* string argument setter */
                        if (onlyArg == String.class) {
                            LOG.trace("Setting '{}' with a new string value of '{}'", sharedVariableName, newSharedVariableValue);
                            setterMethod.invoke(sharedVariableSchema, newSharedVariableValue);
                        /* char array argument setter */
                        } else if (onlyArg.getName().equalsIgnoreCase("[C")) {
                            LOG.trace("Setting '{}' with a new char array value of '{}'", sharedVariableName, newSharedVariableValue);
                            setterMethod.invoke(sharedVariableSchema, newSharedVariableValue.toCharArray());

                        /* long argument setter */
                        } else if (onlyArg.getName().equalsIgnoreCase("long")) {
                            LOG.trace("Setting '{}' with a new long value of '{}'", sharedVariableName, newSharedVariableValue);
                            setterMethod.invoke(sharedVariableSchema, Long.parseLong(newSharedVariableValue));

                        } else {
                            LOG.error("Unknown variable name '{}' found, ignoring SharedVariable update.", sharedVariableName);
                        }

                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);

                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
