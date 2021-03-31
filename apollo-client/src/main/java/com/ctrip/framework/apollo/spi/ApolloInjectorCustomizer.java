package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.internals.DefaultInjector;
import com.ctrip.framework.apollo.internals.Injector;

/**
 * Allow users to inject customized instances, see {@link DefaultInjector#getInstance(java.lang.Class)}
 */
public interface ApolloInjectorCustomizer extends Injector, Ordered {

}
