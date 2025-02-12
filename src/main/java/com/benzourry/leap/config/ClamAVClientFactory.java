package com.benzourry.leap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.solita.clamav.ClamAVClient;

@Component
public class ClamAVClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClamAVClientFactory.class);

    private final String hostname;
    private final int port;
    private final int timeout;

    private final boolean enable;

    public ClamAVClientFactory(
            @Value("${clamd.host}") String hostname,
            @Value("${clamd.port}") int port,
            @Value("${clamd.timeout}") int timeout,
            @Value("${clamd.enable}") boolean enable) {

        logger.debug(" Clam AV hostname :: {}", hostname);
        logger.debug(" Clam AV enable :: {}", enable);
        logger.debug(" Clam AV port number :: {}", port);
        logger.debug(" Clam AV timeout in milliseconds :: {}", timeout);

        this.enable = enable;
        this.hostname = hostname;
        this.port = port;
        this.timeout = timeout;
    }

    public ClamAVClient newClient() {
        System.out.println("hostname:"+hostname);
        return new ClamAVClient(hostname, port, timeout);
    }

    public boolean isEnable() {
        return enable;
    }
}