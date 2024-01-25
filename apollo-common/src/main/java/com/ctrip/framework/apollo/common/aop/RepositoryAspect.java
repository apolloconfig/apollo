/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.common.aop;

import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RepositoryAspect {

  @Pointcut("execution(public * org.springframework.data.repository.Repository+.*(..))")
  public void anyRepositoryMethod() {
  }

  @Around("anyRepositoryMethod()")
  public Object invokeWithCatTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
    String name =
        joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature()
            .getName();
    Transaction catTransaction = Tracer.newTransaction("SQL", name);
    try {
      Object result = joinPoint.proceed();
      catTransaction.setStatus(Transaction.SUCCESS);
      return result;
    } catch (Throwable ex) {
      catTransaction.setStatus(ex);
      throw ex;
    } finally {
      catTransaction.complete();
    }
  }
}
