package com.benzourry.leap;

import com.benzourry.leap.config.AppProperties;
import com.benzourry.leap.utility.audit.AuditorAwareImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableCaching
@EnableRetry
public class LeapApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeapApplication.class, args);
	}

	@Bean
	AuditorAware<String> auditorProvider(){
		return new AuditorAwareImpl();
	}

//	@RestController
//	public class Test{
//		@GetMapping("/px/**")
//		public ResponseEntity<?> proxyPath(ProxyExchange<byte[]> proxy) throws Exception {
//			String path = proxy.path("/px/");
//			return proxy.uri("https://www.google.com/" + path).get();
//		}
//	}

}
