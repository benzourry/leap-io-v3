package com.benzourry.leap.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by user on 9/25/13.
 */
//@ConfigurationProperties(prefix = "instance")
@Component
public class Constant {

    public static final Integer
            UNREAD = 1,
            READ = 2;

    public static final String
            NOTIFICATION_CHANNEL = "";

    public static String UPLOAD_ROOT_DIR;
    public static String LEAP_MAILER;
    public static String UI_BASE_DOMAIN;
    public static String IO_BASE_DOMAIN;
    public static String COGNA_SERVER;
    public static String BROKER_BASE_HTTP;

    @Autowired
    public Constant(@Value("${instance.UPLOAD_ROOT_DIR}") final String UPLOAD_ROOT_DIR,
                    @Value("${instance.LEAP_MAILER}") final String LEAP_MAILER,
                    @Value("${instance.UI_BASE_DOMAIN}") final String UI_BASE_DOMAIN,
                    @Value("${instance.IO_BASE_DOMAIN}") final String IO_BASE_DOMAIN,
                    @Value("${ping.BROKER_BASE_HTTP}") final String BROKER_BASE_HTTP) {
        this.UPLOAD_ROOT_DIR = UPLOAD_ROOT_DIR;
        this.LEAP_MAILER = LEAP_MAILER;
        this.UI_BASE_DOMAIN = UI_BASE_DOMAIN;
        this.IO_BASE_DOMAIN = IO_BASE_DOMAIN;
        this.BROKER_BASE_HTTP = BROKER_BASE_HTTP;
    }


    public static final int ENABLED = 1;
}
