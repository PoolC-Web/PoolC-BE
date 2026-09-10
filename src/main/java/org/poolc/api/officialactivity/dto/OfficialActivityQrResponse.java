package org.poolc.api.officialactivity.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class OfficialActivityQrResponse {
    String checkInUrl;
    String qrImageDataUrl;
    LocalDateTime expiresAt;
}
