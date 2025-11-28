package com.benzourry.leap.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * Created by MohdRazif on 3/2/2017.
 */

public class LogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final HttpStatus httpStatusToReturn;

    public LogoutSuccessHandler(HttpStatus httpStatusToReturn) {
        Assert.notNull(httpStatusToReturn, "The provided HttpStatus must not be null.");
        this.httpStatusToReturn = httpStatusToReturn;
    }

    public LogoutSuccessHandler(String defaultTargetURL) {
        this.httpStatusToReturn = HttpStatus.OK;
        this.setDefaultTargetUrl(defaultTargetURL);
    }

    public LogoutSuccessHandler() {
        this.httpStatusToReturn = HttpStatus.OK;
    }

    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        String redirectUri = request.getContextPath() + "/login?logout";
        if(request.getParameter("redirect_uri")!= null) {
            redirectUri = request.getParameter("redirect_uri");
            setDefaultTargetUrl(redirectUri);
            super.onLogoutSuccess(request, response, authentication);
        }else{
            response.setStatus(this.httpStatusToReturn.value());
            response.getWriter().flush();
            super.onLogoutSuccess(request, response, authentication);
        }

//        response.setStatus(this.httpStatusToReturn.value());
//        response.getWriter().flush();

        response.setHeader("Access-Control-Allow-Origin", "*");

    }
}
