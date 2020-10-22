package com.ctrip.framework.apollo.portal.spi.ctrip;

import com.google.gson.Gson;

import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.MQService;
import com.ctrip.framework.apollo.tracer.Tracer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import javax.annotation.PostConstruct;


public class CtripMQService implements MQService {

  private static final org.apache.commons.lang.time.FastDateFormat
      TIMESTAMP_FORMAT = org.apache.commons.lang.time.FastDateFormat.getInstance("yyyy-MM-dd hh:mm:ss");
  private static final String CONFIG_PUBLISH_NOTIFY_TO_NOC_TOPIC = "ops.noc.record.created";

  private static final Gson GSON = new Gson();

  @Autowired
  private AppService appService;
  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private PortalConfig portalConfig;

  private RestTemplate restTemplate;

  @PostConstruct
  public void init() {
    restTemplate = new RestTemplate();

    SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
    rf.setReadTimeout(portalConfig.readTimeout());
    rf.setConnectTimeout(portalConfig.connectTimeout());

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(
        Arrays.asList(MediaType.APPLICATION_JSON_UTF8, MediaType.APPLICATION_OCTET_STREAM));

    restTemplate.setMessageConverters(Arrays.asList(converter, new FormHttpMessageConverter()));

  }

  @Override
  public void sendPublishMsg(Env env, ReleaseHistoryBO releaseHistory) {
    if (releaseHistory == null) {
      return;
    }

    PublishMsg msg = buildPublishMsg(env, releaseHistory);

    sendMsg(portalConfig.hermesServerAddress(), CONFIG_PUBLISH_NOTIFY_TO_NOC_TOPIC, msg);
  }

  private PublishMsg buildPublishMsg(Env env, ReleaseHistoryBO releaseHistory) {

    PublishMsg msg = new PublishMsg();

    msg.setPriority("中");
    msg.setTool_origin("Apollo");

    String appId = releaseHistory.getAppId();
    App app = appService.load(appId);
    msg.setInfluence_bu(app.getOrgName());
    msg.setAppid(appId);
    msg.setAssginee(releaseHistory.getOperator());
    msg.setOperation_time(TIMESTAMP_FORMAT.format(releaseHistory.getReleaseTime()));
    msg.setDesc(GSON.toJson(releaseService.compare(env, releaseHistory.getPreviousReleaseId(),
                                                   releaseHistory.getReleaseId())));

    return msg;
  }

  private void sendMsg(String serverAddress, String topic, Object msg) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM + ";charset=UTF-8"));
    HttpEntity<Object> request = new HttpEntity<>(msg, headers);

    try {
      //send msg by hermes RestAPI
      restTemplate.postForObject(serverAddress + "/topics/" + topic, request, Object.class);

    } catch (Exception e) {
      Tracer.logError("Send publish msg to hermes failed", e);
    }

  }

  private static class PublishMsg {

    private String assginee;
    private String desc;
    private String operation_time;
    private String tool_origin;
    private String priority;
    private String influence_bu;
    private String appid;


    public String getAssginee() {
      return assginee;
    }

    public void setAssginee(String assginee) {
      this.assginee = assginee;
    }

    public String getDesc() {
      return desc;
    }

    public void setDesc(String desc) {
      this.desc = desc;
    }

    public String getOperation_time() {
      return operation_time;
    }

    public void setOperation_time(String operation_time) {
      this.operation_time = operation_time;
    }

    public String getTool_origin() {
      return tool_origin;
    }

    public void setTool_origin(String tool_origin) {
      this.tool_origin = tool_origin;
    }

    public String getPriority() {
      return priority;
    }

    public void setPriority(String priority) {
      this.priority = priority;
    }

    public String getInfluence_bu() {
      return influence_bu;
    }

    public void setInfluence_bu(String influence_bu) {
      this.influence_bu = influence_bu;
    }

    public String getAppid() {
      return appid;
    }

    public void setAppid(String appid) {
      this.appid = appid;
    }
  }

}
