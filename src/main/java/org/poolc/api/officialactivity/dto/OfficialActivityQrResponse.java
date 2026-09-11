package org.poolc.api.officialactivity.dto;

import lombok.Value;

@Value
public class OfficialActivityQrResponse {
    String checkInUrl;
    String qrImageDataUrl;
}
