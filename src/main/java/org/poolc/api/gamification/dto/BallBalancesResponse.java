package org.poolc.api.gamification.dto;

import lombok.Getter;
@Getter
public class BallBalancesResponse {
    private final long normal;
    private final long master;

    public BallBalancesResponse(long normal, long master) {
        this.normal = normal;
        this.master = master;
    }
}
