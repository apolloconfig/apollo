package com.ctrip.framework.apollo.portal.listener;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginListener {

  @EventListener
  public void onLoginSuccess(AuthenticationSuccessEvent event){
    System.out.println("onLoginSuccess!!!");
    System.out.println("user-->"+event.getAuthentication().getName());
  }

  @EventListener
  public void onLoginFailed(AbstractAuthenticationFailureEvent event){
    System.out.println("onLoginFailed!!!");
    System.out.println("user-->"+event.getAuthentication().getName());
    System.out.println("exception-->"+event.getException().getMessage());
  }


}
