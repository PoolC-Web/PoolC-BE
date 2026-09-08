package org.poolc.api.gamification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class PokemonAssetStorage {
    private static final int CARD_MAX_EDGE = 240;
    private static final int DETAIL_MAX_EDGE = 960;
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final RestTemplate restTemplate = createRestTemplate();
    private volatile S3Client s3Client;

    @Value("${gamification.pokemon-assets.bucket}")
    private String bucket;

    @Value("${gamification.pokemon-assets.public-base-url}")
    private String publicBaseUrl;

    @Value("${gamification.pokemon-assets.region}")
    private String region;

    public PokemonAssetUrls synchronize(Long externalId, String spriteUrl, String shinySpriteUrl) {
        String version = version(spriteUrl, shinySpriteUrl);
        String prefix = "pokemon/" + externalId + "/" + version + "/";
        S3Client client = s3Client();
        boolean requiresShinyAssets = shinySpriteUrl != null && !shinySpriteUrl.isBlank();
        if (!exists(client, prefix + "card.webp") || !exists(client, prefix + "detail.webp")
                || (requiresShinyAssets && (!exists(client, prefix + "shiny-card.webp") || !exists(client, prefix + "shiny-detail.webp")))) {
            storeVariants(client, prefix, spriteUrl, "");
            if (requiresShinyAssets) {
                storeVariants(client, prefix, shinySpriteUrl, "shiny-");
            }
        }

        String baseUrl = trimTrailingSlash(publicBaseUrl) + "/" + prefix;
        return new PokemonAssetUrls(
                baseUrl + "card.webp",
                baseUrl + "detail.webp",
                shinySpriteUrl == null || shinySpriteUrl.isBlank() ? null : baseUrl + "shiny-card.webp",
                shinySpriteUrl == null || shinySpriteUrl.isBlank() ? null : baseUrl + "shiny-detail.webp");
    }

    private void storeVariants(S3Client client, String prefix, String sourceUrl, String namePrefix) {
        byte[] source = restTemplate.getForObject(sourceUrl, byte[].class);
        if (source == null) {
            throw new IllegalStateException("포켓몬 이미지를 내려받지 못했습니다.");
        }
        BufferedImage image = readImage(source);
        put(client, prefix + namePrefix + "card.webp", toWebp(image, CARD_MAX_EDGE));
        put(client, prefix + namePrefix + "detail.webp", toWebp(image, DETAIL_MAX_EDGE));
    }

    private void put(S3Client client, String key, byte[] content) {
        client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/webp")
                        .cacheControl(CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(content));
    }

    private boolean exists(S3Client client, String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    private BufferedImage readImage(byte[] source) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source));
            if (image == null) {
                throw new IllegalStateException("지원하지 않는 포켓몬 이미지 형식입니다.");
            }
            return image;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("포켓몬 이미지를 읽지 못했습니다.", exception);
        }
    }

    private byte[] toWebp(BufferedImage source, int maxEdge) {
        double scale = Math.min(1d, (double) maxEdge / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(resized, "webp", output)) {
                throw new IllegalStateException("WebP 인코더를 찾을 수 없습니다.");
            }
            return output.toByteArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("포켓몬 WebP 이미지 생성에 실패했습니다.", exception);
        }
    }

    private String version(String spriteUrl, String shinySpriteUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((spriteUrl + "|" + (shinySpriteUrl == null ? "" : shinySpriteUrl)).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 8; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("이미지 버전을 계산하지 못했습니다.", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private S3Client s3Client() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = S3Client.builder().region(Region.of(region)).build();
                }
            }
        }
        return s3Client;
    }

    @PreDestroy
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public static class PokemonAssetUrls {
        private final String cardUrl;
        private final String detailUrl;
        private final String shinyCardUrl;
        private final String shinyDetailUrl;

        public PokemonAssetUrls(String cardUrl, String detailUrl, String shinyCardUrl, String shinyDetailUrl) {
            this.cardUrl = cardUrl;
            this.detailUrl = detailUrl;
            this.shinyCardUrl = shinyCardUrl;
            this.shinyDetailUrl = shinyDetailUrl;
        }

        public String getCardUrl() { return cardUrl; }
        public String getDetailUrl() { return detailUrl; }
        public String getShinyCardUrl() { return shinyCardUrl; }
        public String getShinyDetailUrl() { return shinyDetailUrl; }
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(15000);
        return new RestTemplate(requestFactory);
    }
}
