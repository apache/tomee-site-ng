package org.apache.openejb.assembler.classic;

import org.apache.openejb.config.sys.JaxbOpenejb;
import org.apache.openejb.jee.bval.PropertyType;
import org.apache.openejb.jee.bval.ValidationConfigType;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public final class ValidatorBuilder {
    public static final Logger logger = Logger.getInstance(LogCategory.OPENEJB_STARTUP, ValidatorBuilder.class);

    private ValidatorBuilder() {
        // no-op
    }

    public static ValidatorFactory buildFactory(ClassLoader classLoader, ValidationInfo info) {
        return buildFactory(info, classLoader);
    }

    public static ValidationConfigType readConfig(URL url) {
        if (url == null) {
            return null;
        }

        ValidationConfigType validationConfigType;
        try {
            validationConfigType = JaxbOpenejb.unmarshal(ValidationConfigType.class, url.openStream());
       } catch (Throwable t) {
            logger.warning("Unable to create module ValidatorFactory instance.  Using default factory", t);
            return null;
        }

        return validationConfigType;
    }

    public static ValidationInfo getInfo(ValidationConfigType config) {
        ValidationInfo info = new ValidationInfo();
        if (config != null) {
            info.providerClassName = config.getDefaultProvider();
            info.constraintFactoryClass = config.getConstraintValidatorFactory();
            info.traversableResolverClass = config.getTraversableResolver();
            info.messageInterpolatorClass = config.getMessageInterpolator();
            for (PropertyType p : config.getProperty()) {
                info.propertyTypes.put(p.getName(), p.getValue());
            }
            for (JAXBElement<String> element : config.getConstraintMapping()) {
                info.constraintMappings.add(element.getValue());
            }
        }
        return info;
    }

    public static ValidatorFactory buildFactory(ValidationInfo config, ClassLoader classLoader) {
        ValidatorFactory factory = null;
        ClassLoader oldContextLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            if (config == null) {
                factory = Validation.buildDefaultValidatorFactory();
            } else {
                Configuration<?> configuration = getConfig(config);
                factory = configuration.buildValidatorFactory();
                configuration.ignoreXmlConfiguration();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextLoader);
        }
        return factory;
    }

    private static Configuration<?> getConfig(ValidationInfo info) {
        Configuration<?> target = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String providerClassName = info.providerClassName;
        if (providerClassName != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends ValidationProvider> clazz = (Class<? extends ValidationProvider>) classLoader.loadClass(providerClassName);
                target = Validation.byProvider(clazz).configure();
                logger.info("Using " + providerClassName + " as validation provider.");
            } catch (ClassNotFoundException e) {
                logger.warning("Unable to load provider class "+providerClassName, e);
            }
        }
        if (target == null) {
            target = Validation.byDefaultProvider().configure();
        }

        String messageInterpolatorClass = info.messageInterpolatorClass;
        if (messageInterpolatorClass != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<MessageInterpolator> clazz = (Class<MessageInterpolator>) classLoader.loadClass(messageInterpolatorClass);
                target.messageInterpolator(clazz.newInstance());
            } catch (Exception e) {
                logger.warning("Unable to set "+messageInterpolatorClass+ " as message interpolator.", e);
            }
            logger.info("Using " + messageInterpolatorClass + " as message interpolator.");
        }
        String traversableResolverClass = info.traversableResolverClass;
        if (traversableResolverClass != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<TraversableResolver> clazz = (Class<TraversableResolver>) classLoader.loadClass(traversableResolverClass);
                target.traversableResolver(clazz.newInstance());
            } catch (Exception e) {
                logger.warning("Unable to set "+traversableResolverClass+ " as traversable resolver.", e);
            }
            logger.info("Using " + traversableResolverClass + " as traversable resolver.");
        }
        String constraintFactoryClass = info.constraintFactoryClass;
        if (constraintFactoryClass != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<ConstraintValidatorFactory> clazz = (Class<ConstraintValidatorFactory>) classLoader.loadClass(constraintFactoryClass);
                target.constraintValidatorFactory(clazz.newInstance());
            } catch (Exception e) {
                logger.warning("Unable to set "+constraintFactoryClass+ " as constraint factory.", e);
            }
            logger.info("Using " + constraintFactoryClass + " as constraint factory.");
        }
        for (Map.Entry<Object, Object> entry : info.propertyTypes.entrySet()) {
            PropertyType property = new PropertyType();
            property.setName((String) entry.getKey());
            property.setValue((String) entry.getValue());

            if (logger.isDebugEnabled()) {
                logger.debug("Found property '" + property.getName() + "' with value '" + property.getValue());
            }
            target.addProperty(property.getName(), property.getValue());
        }
        for (String mappingFileName : info.constraintMappings) {
            if (logger.isDebugEnabled()) {
                logger.debug("Opening input stream for " + mappingFileName);
            }
            InputStream in = classLoader.getResourceAsStream(mappingFileName);
            if (in == null) {
                throw new ValidationException("Unable to open input stream for mapping file " + mappingFileName);
            }
            target.addMapping(in);
        }

        return target;
    }
}
