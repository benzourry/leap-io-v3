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
    private static final String REDIRECT_PARAM = "redirect_uri";
    private static final String LOGOUT_DEFAULT_REDIRECT = "/login?logout";


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

        response.setHeader("Access-Control-Allow-Origin", "*");

        String redirectUri = request.getContextPath() + "/login?logout";

        if(request.getParameter(REDIRECT_PARAM)!= null) {
            redirectUri = request.getParameter(REDIRECT_PARAM);
        }else{
            response.setStatus(this.httpStatusToReturn.value());
            response.getWriter().flush();
        }

        setDefaultTargetUrl(redirectUri);
        super.onLogoutSuccess(request, response, authentication);

    }
}
