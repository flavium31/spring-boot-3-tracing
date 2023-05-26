package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Slf4j
public class DemoApplication {

	public static void main(String[] args) {
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> route(Handler handler) {
		return RouterFunctions
				.route()
				.GET("/hello", handler::hello)
				.build();
	}

	@Service
	static class Handler {
		public Mono<ServerResponse> hello(ServerRequest serverRequest) {
			return Mono.just("Hello")
					.doOnNext(e -> log.info("Inside the endpoint"))
					.then(Mono.error(() -> new RuntimeException("Error")));
		}
	}

	@Bean
	public GlobalErrorWebExceptionHandler globalErrorHandler(ErrorAttributes errorAttributes,
																			  WebProperties webProperties,
																			  ApplicationContext applicationContext,
																			  ServerCodecConfigurer serverCodecConfigurer) {
		return new GlobalErrorWebExceptionHandler(errorAttributes, webProperties, applicationContext, serverCodecConfigurer);
	}


	public static class GlobalErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

		private final ErrorAttributes errorAttributes;

		public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
											  WebProperties webProperties,
											  ApplicationContext applicationContext,
											  ServerCodecConfigurer serverCodecConfigurer) {
			super(errorAttributes, webProperties.getResources(), new ErrorProperties(), applicationContext);
			super.setMessageReaders(serverCodecConfigurer.getReaders());
			super.setMessageWriters(serverCodecConfigurer.getWriters());
			this.errorAttributes = errorAttributes;
		}

		@Override
		protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
			return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
					.filter((request, responseFunction) -> {
						log.warn("Building error response");
						return ServerResponse.status(500).build().doOnNext(r -> log.error("Error response built"));
					});
		}

		@Override
		protected Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
			log.info("Error");
			return Mono.error(errorAttributes.getError(request));
		}
	}

}
