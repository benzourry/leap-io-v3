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


/*    ### FOR SARAWAK IIC */
//    public static final String UPLOAD_ROOT_DIR = "/var/leap-files";
//    public static final String LEAP_MAILER = "mailer@sarawakiic.org.my";
//    public static final String UI_BASE_DOMAIN = "leap.sarawakiic.org.my";
//    public static final String IO_BASE_DOMAIN = "https://leap-io.sarawakiic.org.my/leap-io/";

/*    ### FOR LEAP */
//    public static final String UPLOAD_ROOT_DIR = "/var/leap-files";
//    public static final String LEAP_MAILER = "mailer@leap.my";
//    public static final String UI_BASE_DOMAIN = "leap.my";
//    public static final String IO_BASE_DOMAIN = "https://io.leap.my/";

/*    ### FOR UNIMAS REKA */
//    public static final String UPLOAD_ROOT_DIR = "/upload/iapp-files";
////    public static final String UPLOAD_ROOT_DIR = "C:/var/leap-files";
//    public static final String LEAP_MAILER = "ia@apps.unimas.my";
//    public static final String UI_BASE_DOMAIN = "ia.unimas.my";
//    public static final String IO_BASE_DOMAIN = "https://rekapi.unimas.my/ia";

/*    ### FOR IREKA */
//    public static final String UPLOAD_ROOT_DIR = "/upload/reka-files";
//    public static final String LEAP_MAILER = "mailer@ireka.my";
//    public static final String UI_BASE_DOMAIN = "ireka.my";
//    public static final String IO_BASE_DOMAIN = "https://io.ireka.my";

/*    ### FOR KBORNEO */
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
//        System.out.println("~~~~~~~~~~~~:"+UI_BASE_DOMAIN);
        this.UPLOAD_ROOT_DIR = UPLOAD_ROOT_DIR;
        this.LEAP_MAILER = LEAP_MAILER;
        this.UI_BASE_DOMAIN = UI_BASE_DOMAIN;
        this.IO_BASE_DOMAIN = IO_BASE_DOMAIN;
        this.BROKER_BASE_HTTP = BROKER_BASE_HTTP;
//        this.COGNA_SERVER = COGNA_SERVER;
    }

    /*    ### FOR KBORNEO */
//    public static final String UPLOAD_ROOT_DIR = "/upload/leap-files";
//    public static final String LEAP_MAILER = "mailer@kborneo.my";
//    public static final String UI_BASE_DOMAIN = "kborneo.my";
//    public static final String IO_BASE_DOMAIN = "https://io.kborneo.my";

/*    ### FOR REKA JLS */
//    public static final String UPLOAD_ROOT_DIR = "/data/reka-files";
//    public static final String LEAP_MAILER = "mailer@blams.jls.gov.my";
//    public static final String UI_BASE_DOMAIN = "reka.jls.gov.my";
//    public static final String IO_BASE_DOMAIN = "https://reka-io.jls.gov.my";


    public static final int ENABLED = 1;
}
