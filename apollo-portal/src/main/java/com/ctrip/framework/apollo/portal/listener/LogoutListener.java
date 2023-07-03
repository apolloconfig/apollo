package com.ctrip.framework.apollo.portal.listener;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LogoutListener {

  @EventListener
  public void onLogoutSuccess(LogoutSuccessEvent event){
    System.out.println("onLogoutSuccess!!!");
    System.out.println("user-->"+event.getAuthentication().getName());
  }

}
