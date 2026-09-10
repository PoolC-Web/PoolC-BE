package org.poolc.api.book.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoBookSearchResponse {

    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private String title;
        private String url;
        private String thumbnail;
        private List<String> authors;
        private Integer sale_price;
        private Integer price;
        private String publisher;
        private String isbn;
        private String contents;
        private String datetime;

        public BookApiResponse toBookApiResponse() {
            return BookApiResponse.builder()
                    .title(title)
                    .link(url)
                    .image(thumbnail)
                    .author(authors == null ? "" : authors.stream().collect(Collectors.joining(", ")))
                    .discount(sale_price != null && sale_price > 0 ? sale_price : price)
                    .publisher(publisher)
                    .isbn(isbn)
                    .description(contents)
                    .pubdate(toDate(datetime))
                    .build();
        }

        private String toDate(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            try {
                return OffsetDateTime.parse(value).toLocalDate().toString();
            } catch (Exception ignored) {
                return value.length() >= 10 ? value.substring(0, 10) : value;
            }
        }
    }
}
