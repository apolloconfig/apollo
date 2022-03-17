package com.ctrip.framework.apollo.biz.utils;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityManagerUtilTest {

	@InjectMocks
	private EntityManagerUtil entityManagerUtil;

	@Mock
	private EntityManagerFactory entityManagerFactory;

	@Test
	public void testCloseEntityManager() {
		entityManagerUtil.closeEntityManager();

	}

}
