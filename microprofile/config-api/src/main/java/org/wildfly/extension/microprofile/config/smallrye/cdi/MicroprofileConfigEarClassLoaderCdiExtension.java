/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.cdi;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import io.smallrye.config.inject.ConfigProducerClassLoaderFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

public class MicroprofileConfigEarClassLoaderCdiExtension implements Extension {
    private final Set<ClassLoader> classLoaders = new HashSet<>();

    public MicroprofileConfigEarClassLoaderCdiExtension() {
    }

    public void addClassLoader(ClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    public void registerClassLoaderFactory(@Observes AfterBeanDiscovery abd) {
        System.out.println("======> Adding bean");
        abd.addBean().scope(Dependent.class)
                .beanClass(ConfigProducerClassLoaderFactory.class)
                .addType(ConfigProducerClassLoaderFactory.class)
                .qualifiers(Default.Literal.INSTANCE)
                .produceWith(instance -> (ConfigProducerClassLoaderFactory) ip -> {
                    System.out.println("======> Using bean");
                    if (ip != null && ip.getBean() != null) {
                        ClassLoader cl = WildFlySecurityManager.getClassLoaderPrivileged(ip.getBean().getClass());
                        System.out.println("======> InjectionPoint CL " + cl);
                        return cl;
                    }
                    return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                });
    }

//    public void veto(@Observes ProcessAnnotatedType<ConfigProducer> event, BeanManager beanManager) {
//        // Don't install the ConfigProducer
//        if (event.getAnnotatedType().getJavaClass().equals(ConfigProducer.class)) {
//            System.out.println("Vetoing bean for " + beanManager);
//            // Disable for now since it breaks everything
//            event.veto();
//        }
//    }
//
//
//    public void registerConfigProducerSimple(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
//        System.out.println("Adding bean for " + beanManager);
//        // If I make this ApplicationScoped it fails with
//        // 10:27:46,343 ERROR [org.jboss.resteasy.core.providerfactory.DefaultExceptionMapper] (default task-3) RESTEASY002375: Error processing request GET /guide-maven-multimodules-war - io.openliberty.guides.multimodules.web.SampleResource.getAllConfig: org.jboss.weld.exceptions.IllegalArgumentException: WELD-001569: Cannot inject injection point metadata in a non @Dependent bean: Configurator Bean [class io.smallrye.config.SmallRyeConfig, types: Object, SmallRyeConfig, Config, qualifiers: @Any @Default]
//        // Although this worked when I modified the ConfigProducer method
//        abd.addBean().scope(Dependent.class)
////                .alternative(true)
////                .priority(10)
//                .beanClass(SmallRyeConfig.class)
//                .addTypes(SmallRyeConfig.class, Config.class)
//                .produceWith(instance -> {
//                    InjectionPoint ip = getInjectedInjectionPoint(instance);
//                    ClassLoader cl = getClassLoaderForInjectionPoint(ip);
//                    return ConfigProvider.getConfig(cl).unwrap(SmallRyeConfig.class);
//                });
//
//        registerSimpleConfigPropertyProducer(abd, String.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Long.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Integer.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Float.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Double.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Boolean.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Short.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Byte.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, Character.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, OptionalInt.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, OptionalLong.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, OptionalDouble.class, ConfigProducerUtil::getValue);
//        registerSimpleConfigPropertyProducer(abd, ConfigValue.class, ConfigProducerUtil::getConfigValue);
//    }
//
//    public <T> void registerConfigProducerGenericBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
//        registerGenericConfigPropertyProducer(abd, Optional.class,
//                new TypeLiteral<Optional<T>>() {
//
//                }, ConfigProducerUtil::getValue);
//        registerGenericConfigPropertyProducer(abd, Supplier.class,
//                new TypeLiteral<Supplier<T>>() {
//                },
//                (ip, config) -> () -> ConfigProducerUtil.getValue(ip, config));
//        registerGenericConfigPropertyProducer(abd, Set.class,
//                new TypeLiteral<Set<T>>() {
//                },
//                ConfigProducerUtil::getValue);
//        registerGenericConfigPropertyProducer(abd, List.class,
//                new TypeLiteral<List<T>>() {
//                },
//                ConfigProducerUtil::getValue);
//    }
//
//    public <K, V> void registerConfigProducerGenericMapBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
//        registerGenericConfigPropertyProducer(abd, Map.class,
//                new TypeLiteral<Map<K, V>>() {
//                },
//                ConfigProducerUtil::getValue);
//    }
//
//
//    private <T> void registerSimpleConfigPropertyProducer(
//            AfterBeanDiscovery abd, Class<T> type, BiFunction<InjectionPoint, SmallRyeConfig, T> function) {
//
//        abd.addBean().scope(Dependent.class)
//                .beanClass(type)
//                .addTypes(type)
//                .addQualifier(ConfigPropertyLiteral.INSTANCE)
//                .produceWith(instance -> {
//                    InjectionPoint ip = getInjectedInjectionPoint(instance);
//                    SmallRyeConfig config = getInjectedConfig(instance);
//                    return function.apply(ip, config);
//                });
//    }
//
//    private <T> void registerGenericConfigPropertyProducer (
//            AfterBeanDiscovery abd, Class<T> type, TypeLiteral<?> typeLiteral, BiFunction<InjectionPoint, SmallRyeConfig, T> function) {
//
//        abd.addBean().scope(Dependent.class)
//                .beanClass(type)
//                .addType(typeLiteral)
//                .addQualifier(ConfigPropertyLiteral.INSTANCE)
//                .produceWith(instance -> {
//                    InjectionPoint ip = getInjectedInjectionPoint(instance);
//                    SmallRyeConfig config = getInjectedConfig(instance);
//                    return function.apply(ip, config);
//                });
//
//    }
//
//    private InjectionPoint getInjectedInjectionPoint(Instance<Object> instance) {
//        InjectionPoint ip = instance.select(InjectionPoint.class).get();
//        System.out.println("---> IP: " + ip);
//        return ip;
//    }
//
//    private SmallRyeConfig getInjectedConfig(Instance<Object> instance) {
//        SmallRyeConfig config = instance.select(SmallRyeConfig.class).get();
//        return config;
//    }
//
//    private ClassLoader getClassLoaderForInjectionPoint(InjectionPoint ip) {
//        ClassLoader cl = null;
//        if (ip.getBean() != null) {
//            System.out.println("---> Bean: " + ip.getBean());
//            ClassLoader test = ip.getBean().getBeanClass().getClassLoader(); // With security manager
//            if (classLoaders.contains(test)) {
//                System.out.println("---> CL: " + test);
//                return cl;
//            }
//        }
//        System.out.println("---> TCCL Fallback");
//        return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
//    }
//
//    private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {
//        static final ConfigPropertyLiteral INSTANCE = new ConfigPropertyLiteral();
//        @Override
//        public String name() {
//            return "";
//        }
//
//        @Override
//        public String defaultValue() {
//            return "";
//        }
//    }
}
