package com.ctrip.framework.apollo.audit.spi;

import com.ctrip.framework.apollo.audit.context.AuditSpanContext;

public interface RootSpanGenerator {

  AuditSpanContext generate();
}
