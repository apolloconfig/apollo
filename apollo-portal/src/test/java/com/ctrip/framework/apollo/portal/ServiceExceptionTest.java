package com.ctrip.framework.apollo.portal;

import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.portal.controller.AppController;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.google.gson.Gson;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ServiceExceptionTest extends AbstractUnitTest {

	@InjectMocks
	private AppController appController;
	@Mock
	private AppService appService;

	private static final Gson GSON = new Gson();


	@Test
	public void testAdminServiceException() {
		String errorMsg = "No available admin service";
		String errorCode = "errorCode";
		String status = "500";

		Map<String, Object> errorAttributes = new LinkedHashMap<>();
		errorAttributes.put("status", status);
		errorAttributes.put("message", errorMsg);
		errorAttributes.put("timestamp",
												LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		errorAttributes.put("exception", ServiceException.class.getName());
		errorAttributes.put("errorCode", errorCode);

		HttpStatusCodeException adminException =
			new HttpServerErrorException(
				HttpStatus.INTERNAL_SERVER_ERROR, "admin server error", GSON.toJson(errorAttributes).getBytes(),
				Charset.defaultCharset()
			);

		when(appService.createAppInLocal(any())).thenThrow(adminException);

		AppModel app = generateSampleApp();
		try {
			appController.create(app);
		} catch (HttpStatusCodeException e) {
			@SuppressWarnings("unchecked")
			Map<String, String> attr = new Gson().fromJson(e.getResponseBodyAsString(), Map.class);
			Assert.assertEquals(errorMsg, attr.get("message"));
			Assert.assertEquals(errorCode, attr.get("errorCode"));
			Assert.assertEquals(status, attr.get("status"));
		}
	}

	private AppModel generateSampleApp() {
		AppModel app = new AppModel();
		app.setAppId("someAppId");
		app.setName("someName");
		app.setOrgId("someOrgId");
		app.setOrgName("someOrgNam");
		app.setOwnerName("someOwner");
		return app;
	}
}
