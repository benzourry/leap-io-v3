package com.benzourry.leap;

import com.benzourry.leap.config.AppProperties;
import com.benzourry.leap.config.DynamicConfigPropertySource;
import com.benzourry.leap.repository.DynamicConfigRepository;
import com.benzourry.leap.utility.audit.AuditorAwareImpl;
import com.benzourry.leap.utility.export.AbstractCsvView;
import com.benzourry.leap.utility.export.CsvView;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
//@EnableAspectJAutoProxy(exposeProxy = true)
@EnableCaching
@EnableRetry
public class LeapApplication {

//	static {
//		try {
//			System.loadLibrary("libtokenizers.so");
//		} catch (UnsatisfiedLinkError ignore) {
//            // After using spring-dev-tools, the context will be loaded multiple times, so here will throw the exception that the link library has been loaded.
//            // If there is this exception, the link library has been loaded, you can directly swallow the exception.
//		}
//	}

	public static void main(String[] args) {
//		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
		SpringApplication.run(LeapApplication.class, args);
	}

	@Component
	static class ConfigDynamicConfigPropertySource implements SmartInitializingSingleton {
		private final ConfigurableEnvironment environment;
		private final DynamicConfigRepository dynamicConfigDao;

		public ConfigDynamicConfigPropertySource(ConfigurableEnvironment environment, DynamicConfigRepository dynamicConfigDao) {
			this.environment = environment;
			this.dynamicConfigDao = dynamicConfigDao;
		}

		@Override
		public void afterSingletonsInstantiated() {
			environment.getPropertySources().addLast(new DynamicConfigPropertySource("all_prop",dynamicConfigDao));
		}
	}

	@Bean
	AuditorAware<String> auditorProvider(){
		return new AuditorAwareImpl();
	}

	@Bean
	public AbstractCsvView csvView() {
		return new CsvView();
	}

//	@Bean
//	public RestTemplate restTemplate() {
//		final RestTemplate restTemplate = new RestTemplate();
//
//		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
//		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//		converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
//		messageConverters.add(converter);
//		restTemplate.setMessageConverters(messageConverters);
//
//		return restTemplate;
//	}

	@RestController
	public class Test{
		@GetMapping("/px/**")
		public ResponseEntity<?> proxyPath(ProxyExchange<byte[]> proxy) throws Exception {
			String path = proxy.path("/px/");
			return proxy.uri("https://www.google.com/" + path).get();
		}
	}


//	@Bean
//	public CommonsRequestLoggingFilter requestLoggingFilter() {
//		CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
//		loggingFilter.setIncludeClientInfo(true);
//		loggingFilter.setIncludeQueryString(true);
//		loggingFilter.setIncludePayload(true);
//		loggingFilter.setMaxPayloadLength(64000);
//		return loggingFilter;
//	}
}
