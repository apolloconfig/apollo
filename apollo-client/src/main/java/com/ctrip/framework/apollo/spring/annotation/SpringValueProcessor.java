package com.ctrip.framework.apollo.spring.annotation;

import com.ctrip.framework.apollo.spring.auto.SpringValue;
import com.ctrip.framework.foundation.Foundation;
import com.ctrip.framework.foundation.spi.provider.ApplicationProvider;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spring value processor of field or method which has @Value.
 *
 * @author github.com/zhegexiaohuozi  seimimaster@gmail.com
 * @since 2017/12/20.
 */
public class SpringValueProcessor implements BeanPostProcessor, PriorityOrdered {
    private Pattern pattern = Pattern.compile("\\$\\{([^:]*)\\}:?(.*)");
    private static Multimap<String, SpringValue> monitor = LinkedListMultimap.create();
    private static ApplicationProvider applicationProvider = Foundation.app();
    private Logger logger = LoggerFactory.getLogger(SpringValueProcessor.class);

    public static Multimap<String, SpringValue> monitor() {
        return monitor;
    }

    public static boolean enable(){
        return applicationProvider.isAutoUpdateEnable();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        boolean enabled = enable();
        if (enabled){
            Class clazz = bean.getClass();
            processFields(bean, findAllField(clazz));
            processMethods(bean, findAllMethod(clazz));
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private void processFields(Object bean, List<Field> declaredFields) {
        for (Field field : declaredFields) {
            // regist @Value on field
            Value value = field.getAnnotation(Value.class);
            if (value == null) {
                continue;
            }
            Matcher matcher = pattern.matcher(value.value());
            if (matcher.matches()) {
                String key = matcher.group(1);
                monitor.put(key, SpringValue.create(key,bean, field));
                logger.info("Listening apollo key = {}", key);
            }
        }
    }

    private void processMethods(final Object bean, List<Method> declaredMethods) {
        for (final Method method : declaredMethods) {
            //regist @Value on method
            Value value = method.getAnnotation(Value.class);
            if (value == null) {
                continue;
            }
            Matcher matcher = pattern.matcher(value.value());
            if (matcher.matches()) {
                String key = matcher.group(1);
                monitor.put(key, SpringValue.create(key,bean, method));
                logger.info("Listening apollo key = {}", key);
            }
        }
    }

    @Override
    public int getOrder() {
        //make it as late as possible
        return Ordered.LOWEST_PRECEDENCE;
    }


    private List<Field> findAllField(Class clazz) {
        final List<Field> res = new LinkedList<>();
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                res.add(field);
            }
        });
        return res;
    }

    private List<Method> findAllMethod(Class clazz) {
        final List<Method> res = new LinkedList<>();
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                res.add(method);
            }
        });
        return res;
    }
}
