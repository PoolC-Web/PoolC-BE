package org.poolc.api.book.client;

import lombok.RequiredArgsConstructor;
import org.poolc.api.book.dto.response.BookApiResponse;
import org.poolc.api.book.dto.response.KakaoBookSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KakaoBookClient implements BookClient {

    private static final int PAGE_SIZE = 10;

    @Value("${kakao.book.api.url}")
    private String url;

    @Value("${kakao.book.rest-api-key}")
    private String restApiKey;

    private final RestTemplate restTemplate;

    @Override
    public List<BookApiResponse> searchBooks(String query, int page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + restApiKey);

        URI requestUri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("query", query)
                .queryParam("page", page + 1)
                .queryParam("size", PAGE_SIZE)
                .build()
                .encode()
                .toUri();

        KakaoBookSearchResponse response = restTemplate.exchange(
                requestUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                KakaoBookSearchResponse.class
        ).getBody();

        if (response == null || response.getDocuments() == null) {
            return List.of();
        }

        return response.getDocuments().stream()
                .filter(Objects::nonNull)
                .map(KakaoBookSearchResponse.Document::toBookApiResponse)
                .collect(Collectors.toList());
    }
}
