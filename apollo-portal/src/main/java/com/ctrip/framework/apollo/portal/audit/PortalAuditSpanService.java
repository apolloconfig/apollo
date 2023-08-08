package com.ctrip.framework.apollo.portal.audit;

import com.ctrip.framework.apollo.audit.constants.ApolloAuditHttpHeader;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class PortalAuditSpanService implements ApolloAuditSpanService {

  private final UserInfoHolder userInfoHolder;

  public PortalAuditSpanService(UserInfoHolder userInfoHolder) {
    this.userInfoHolder = userInfoHolder;
  }

  @Override
  public ApolloAuditSpanContext tryToGetParentSpanContext() {
    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if(servletRequestAttributes == null) {
      return null;
    }
    HttpServletRequest request = servletRequestAttributes.getRequest();
    String traceId = request.getHeader(ApolloAuditHttpHeader.TRACE_ID);
    String spanId = request.getHeader(ApolloAuditHttpHeader.SPAN_ID);
    String operator = request.getHeader(ApolloAuditHttpHeader.OPERATOR);
    if(Objects.isNull(traceId) || Objects.isNull(spanId) || Objects.isNull(operator)){
      return null;
    } else {
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId);
      context.setOperator(operator);
      return context;
    }
  }

  @Override
  public String getOperator() {
    return userInfoHolder.getUser().getUserId();
  }
}
