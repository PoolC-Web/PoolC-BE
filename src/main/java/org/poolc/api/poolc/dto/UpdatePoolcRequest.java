package org.poolc.api.poolc.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public class UpdatePoolcRequest {
    private final String presidentName;
    private final String phoneNumber;
    private final String location;
    private final String locationUrl;
    private final String introduction;
    private final String mainImageUrl;
    private final Boolean isSubscriptionPeriod;
    private final String applyUri;
    private final Integer minimumActivityHours;

    @JsonCreator
    public UpdatePoolcRequest(String presidentName, String phoneNumber, String location, String location_url, String introduction, String mainImageUrl, Boolean isSubscriptionPeriod, String applyLinkUri, Integer minimumActivityHours) {
        this.presidentName = presidentName;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.locationUrl = location_url;
        this.introduction = introduction;
        this.mainImageUrl = mainImageUrl;
        this.isSubscriptionPeriod = isSubscriptionPeriod;
        this.applyUri = applyLinkUri;
        this.minimumActivityHours = minimumActivityHours;
    }

    public UpdatePoolcRequest(String presidentName, String phoneNumber, String location, String locationUrl, String introduction, String mainImageUrl, Boolean isSubscriptionPeriod, String applyUri) {
        this(presidentName, phoneNumber, location, locationUrl, introduction, mainImageUrl, isSubscriptionPeriod, applyUri, null);
    }
}
