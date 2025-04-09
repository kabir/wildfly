/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.inject.ConfigProducerUtil;
import io.smallrye.config.inject.InjectionMessages;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.wildfly.security.manager.WildFlySecurityManager;

@ApplicationScoped
public class EarConfigProducer {
    @Produces
    protected SmallRyeConfig getConfig() {
        return ConfigProvider.getConfig(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged()).unwrap(SmallRyeConfig.class);
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected String produceStringConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Long getLongValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Integer getIntegerValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Float produceFloatConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Double produceDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Boolean produceBooleanConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Short produceShortConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Byte produceByteConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected Character produceCharacterConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Optional<T> produceOptionalConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Supplier<T> produceSupplierConfigProperty(InjectionPoint ip) {
        return () -> ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> Set<T> producesSetConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <T> List<T> producesListConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected <K, V> Map<K, V> producesMapConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalInt produceOptionalIntConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalLong produceOptionalLongConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected OptionalDouble produceOptionalDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, getConfig());
    }

    @Dependent
    @Produces
    @ConfigProperty
    protected ConfigValue produceConfigValue(InjectionPoint ip) {
        String name = getName(ip);
        if (name == null) {
            return null;
        } else {
            ConfigValue configValue = getConfig().getConfigValue(name);
            if (configValue.getRawValue() == null) {
                configValue = configValue.withValue(getDefaultValue(ip));
            }

            return configValue;
        }
    }

    private static String getName(InjectionPoint injectionPoint) {
        for(Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = (ConfigProperty)qualifier;
                return getConfigKey(injectionPoint, configProperty);
            }
        }

        return null;
    }

    private static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.trim().isEmpty()) {
            return key;
        } else {
            if (ip.getAnnotated() instanceof AnnotatedMember) {
                AnnotatedMember<?> member = (AnnotatedMember)ip.getAnnotated();
                AnnotatedType<?> declaringType = member.getDeclaringType();
                if (declaringType != null) {
                    String[] parts = declaringType.getJavaClass().getCanonicalName().split("\\.");
                    StringBuilder sb = new StringBuilder(parts[0]);

                    for(int i = 1; i < parts.length; ++i) {
                        sb.append(".").append(parts[i]);
                    }

                    sb.append(".").append(member.getJavaMember().getName());
                    return sb.toString();
                }
            }

            throw InjectionMessages.msg.noConfigPropertyDefaultName(ip);
        }
    }

    private static String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                String str = ((ConfigProperty) qualifier).defaultValue();
                if (!ConfigProperty.UNCONFIGURED_VALUE.equals(str)) {
                    return str;
                }
                Class<?> rawType = rawTypeOf(injectionPoint.getType());
                if (rawType.isPrimitive()) {
                    if (rawType == char.class) {
                        return null;
                    } else if (rawType == boolean.class) {
                        return "false";
                    } else {
                        return "0";
                    }
                }
                return null;
            }
        }
        return null;
    }

    private static <T> Class<T> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return (Class<T>) Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else {
            throw InjectionMessages.msg.noRawType(type);
        }
    }


    public static boolean isClassHandledByConfigProducer(Type requiredType) {
        return requiredType == String.class
                || requiredType == Boolean.class
                || requiredType == Boolean.TYPE
                || requiredType == Integer.class
                || requiredType == Integer.TYPE
                || requiredType == Long.class
                || requiredType == Long.TYPE
                || requiredType == Float.class
                || requiredType == Float.TYPE
                || requiredType == Double.class
                || requiredType == Double.TYPE
                || requiredType == Short.class
                || requiredType == Short.TYPE
                || requiredType == Byte.class
                || requiredType == Byte.TYPE
                || requiredType == Character.class
                || requiredType == Character.TYPE
                || requiredType == OptionalInt.class
                || requiredType == OptionalLong.class
                || requiredType == OptionalDouble.class
                || requiredType == Supplier.class
                || requiredType == ConfigValue.class
                || requiredType == org.eclipse.microprofile.config.ConfigValue.class;
    }
}
