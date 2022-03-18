package com.ctrip.framework.apollo.biz.utils;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Ayush Jha
 */

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
	// 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
	//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(git token)
}
