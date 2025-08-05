/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.UserLoginDto;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.entity.vo.UserToken;
import com.ctrip.framework.apollo.portal.service.LoginService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RestController
public class LoginController {
    private final LoginService loginService;
    public LoginController(final LoginService loginService) {
        this.loginService = loginService;
    }
    @PostMapping("/login")
    public UserToken login(@RequestBody UserLoginDto userLoginDto, HttpServletResponse response) {
        RequestPrecondition.checkArgumentsNotEmpty(userLoginDto.getUsername(), userLoginDto.getPassword());
        return loginService.login(userLoginDto,response);
    }
    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        loginService.logout(request, response);
    }
    @GetMapping("/refreshToken")
    public UserToken refreshToken(HttpServletRequest request) {
        return loginService.refreshToken(request);
    }


}
