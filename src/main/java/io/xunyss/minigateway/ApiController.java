package io.xunyss.minigateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Set;

@RestController
public class ApiController {

	private final Gson gson = new GsonBuilder()
			.setPrettyPrinting()  // 들여쓰기와 줄바꿈 적용
			.create();

	private final String baseUrl;
	private final WebClient webClient;

	public ApiController(@Value("${api.baseUrl}") String baseUrl) {
		this.baseUrl = baseUrl;
		this.webClient = WebClient.create(baseUrl);
	}

	// Tomcat 에서 최종 response 에 "Connection" 에더가 "keep-alive, keep-alive" 로 변경되고, "Transfer-Encoding" 헤어가 한번 더 붙는 현상 발생
	// Jetty server 로 변경 (build.gradle.kts)
	// Jetty server 에서도 최종 response 에 "Date" 헤더를 한번 더 붙이는 현상 있음
	private static final Set<String> excludeHeaders =
			Set.of(HttpHeaders.DATE);


	@RequestMapping("/**")
	public ResponseEntity<String> index(HttpServletRequest request, @RequestBody String body) {
		String method = request.getMethod();

		// WebClient 요청 준비
		WebClient.RequestBodySpec requestSpec = webClient
				.method(HttpMethod.valueOf(method))
				.uri(request.getRequestURI());

		// 모든 헤더 복사
		for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = headerNames.nextElement();
			// Host 헤더는 제외 (WebClient가 자동으로 설정)
			if (!"host".equalsIgnoreCase(headerName)) {
				Enumeration<String> headerValues = request.getHeaders(headerName);
				while (headerValues.hasMoreElements()) {
					requestSpec = requestSpec.header(headerName, headerValues.nextElement());
				}
			}
		}

		// 요청 바디 설정 및 요청 실행
		WebClient.RequestHeadersSpec<?> finalRequest;
		if (body != null && !body.isEmpty()) {
			finalRequest = requestSpec.bodyValue(body);
		}
        else {
			finalRequest = requestSpec;
		}

		log("REQUEST >> " + baseUrl + request.getRequestURI());
		log(format(body));

		// 요청 실행 및 응답 반환
		ResponseEntity<String> responseEntity = finalRequest
				.exchangeToMono(response -> response.bodyToMono(String.class)
						.map(responseBody -> ResponseEntity
								.status(response.statusCode())
//								.headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
								.headers(httpHeaders -> response.headers().asHttpHeaders().forEach((headerName, headerValues) -> {
                                    if (!excludeHeaders.contains(headerName)) {
                                        httpHeaders.put(headerName, headerValues);
                                    }
                                }))
								.body(responseBody)))
				.block();

		if (responseEntity != null) {
			log("RESPONSE >> " + responseEntity.getStatusCode());
			log(format(responseEntity.getBody()));
		}
		return responseEntity;
	}


	private void log(Object o) {
		System.out.println(o);
	}

	private String format(String json) {
		try {
			return gson.toJson(gson.fromJson(json, Object.class));
		}
		catch (JsonParseException e) {
			return json;
		}
	}
}
