package com.ctrip.framework.apollo.spring.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;

/**
 * @author Ayush Jha 
 */

@RunWith(MockitoJUnitRunner.class)
public class BeanRegistrationUtilTest {

	@InjectMocks
	private BeanRegistrationUtil beanRegistrationUtil;
	private BeanDefinitionRegistry someRegistry;
	String someBeanName = "someBean";

	@Before
	public void setUp() {
		someRegistry = new SimpleBeanDefinitionRegistry();
	}

	@Test
	public void registerBeanDefinitionIfNotExistsTest() {
		someRegistry.registerBeanDefinition(someBeanName, Mockito.mock(BeanDefinition.class));
		assertFalse(
				BeanRegistrationUtil.registerBeanDefinitionIfNotExists(someRegistry, someBeanName, getClass(), null));
		assertFalse(BeanRegistrationUtil.registerBeanDefinitionIfNotExists(someRegistry, someBeanName, getClass()));

	}

	@Test
	public void registerBeanDefinitionIfNotExistsBeanNotPresentTest() {
		someRegistry.registerBeanDefinition("someAnotherBean", Mockito.mock(BeanDefinition.class));
		assertTrue(
				BeanRegistrationUtil.registerBeanDefinitionIfNotExists(someRegistry, someBeanName, getClass(), null));

	}

	@Test
	public void registerBeanDefinitionIfNotExistsWithExtPropTest() {
		someRegistry.registerBeanDefinition("someAnotherBean", Mockito.mock(BeanDefinition.class));
		Map<String, Object> extraPropertyValues = new ConcurrentHashMap<>();
		extraPropertyValues.put(someBeanName, "someProperty");
		assertTrue(BeanRegistrationUtil.registerBeanDefinitionIfNotExists(someRegistry, someBeanName, getClass(),
				extraPropertyValues));

	}

}
