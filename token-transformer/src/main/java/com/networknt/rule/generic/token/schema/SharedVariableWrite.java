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
                final var value = (String) sourceData.get(sourceEntry.getSource());
                final var matcher = VARIABLE_PATTERN.matcher(sourceEntry.getDestination());

                if (matcher.find()) {
                    final var variable = matcher.group(1);
                    final var searchArr = variable.split("\\.");
                    if (searchArr.length == 2) {
                        pushToSharedVariable(sharedVariableSchema, beaninfo, searchArr[1], value);
                    }
                }
            }
        }
    }

    private static void pushToSharedVariable(final SharedVariableSchema sharedVariableSchema, final BeanInfo beanInfo, final String sharedVariableName, final String newSharedVariableValue) {
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        Method setterMethod;

        for (PropertyDescriptor pd : pds) {

            if (pd.getName().equalsIgnoreCase(sharedVariableName)) {
                setterMethod = pd.getWriteMethod(); // For Setter Method

                if (setterMethod != null) {
                    try {
                        final var argTypes = setterMethod.getParameterTypes();

                        if (argTypes.length != 1)
                            continue;

                        final var onlyArg = argTypes[0];

                        /* string argument setter */
                        if (onlyArg == String.class) {
                            setterMethod.invoke(sharedVariableSchema, newSharedVariableValue);

                        /* char array argument setter */
                        } else if (onlyArg.getName().equalsIgnoreCase("[C")) {
                            setterMethod.invoke(sharedVariableSchema, newSharedVariableValue.toCharArray());

                        /* long argument setter */
                        } else if (onlyArg.getName().equalsIgnoreCase("long")) {
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
