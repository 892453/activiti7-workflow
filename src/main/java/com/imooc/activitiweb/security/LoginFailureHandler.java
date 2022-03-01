package com.imooc.activitiweb.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.activitiweb.util.AjaxResponse;
import com.imooc.activitiweb.util.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录失败的处理方法
 */

@Component("loginFailureHandler")
public class LoginFailureHandler implements AuthenticationFailureHandler{
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        logger.info("登录失败");
//        设置登录失败后返回的状态码为 500  HttpStatus.INTERNAL_SERVER_ERROR
        httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        httpServletResponse.setContentType("application/json;charset=UTF-8");



        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(
                AjaxResponse.AjaxData(GlobalConfig.ResponseCode.ERROR.getCode(),
                GlobalConfig.ResponseCode.ERROR.getDesc(),
                        "登录失败，原因："+e.getMessage()
                )));
    }
}



