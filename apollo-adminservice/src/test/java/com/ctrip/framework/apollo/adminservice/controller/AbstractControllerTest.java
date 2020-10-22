package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.AdminServiceTestConfiguration;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdminServiceTestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractControllerTest {

  @Autowired
  private HttpMessageConverters httpMessageConverters;

  protected RestTemplate restTemplate = (new TestRestTemplate()).getRestTemplate();

  @PostConstruct
  private void postConstruct() {
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    restTemplate.setMessageConverters(httpMessageConverters.getConverters());
  }

  @Value("${local.server.port}")
  protected int port;

  protected String url(String path) {
    return "http://localhost:" + port + path;
  }
}
