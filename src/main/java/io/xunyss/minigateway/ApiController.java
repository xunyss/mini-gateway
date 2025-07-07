package io.xunyss.minigateway;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Enumeration;

@RestController
public class ApiController {

	private final Gson gson = new GsonBuilder()
			.setPrettyPrinting()  // 들여쓰기와 줄바꿈 적용
			.create();

	private final String apiHost;
	private final WebClient webClient;

	public ApiController(@Value("${api.host}") String apiHost) {
		this.apiHost = apiHost;
		this.webClient = WebClient.create(apiHost);
	}

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

		log("REQUEST >> " + apiHost + request.getRequestURI());
		log(format(body));

		// 요청 실행 및 응답 반환
		ResponseEntity<String> responseEntity = finalRequest
				.exchangeToMono(response -> response.bodyToMono(String.class)
						.map(responseBody -> ResponseEntity
								.status(response.statusCode())
								.headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
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
