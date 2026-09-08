package org.poolc.api.gamification.dto;

import lombok.Getter;
import org.poolc.api.gamification.domain.AchievementProgress;

@Getter
public class AchievementResponse {
    private final String key;
    private final String type;
    private final String title;
    private final String description;
    private final int target;
    private final int progress;
    private final String rewardBallType;
    private final int rewardAmount;
    private final boolean claimed;
    private final int claimedCount;
    private final int claimableCount;

    public AchievementResponse(String key, String type, String title, String description,
                               int target, AchievementProgress progress, String rewardBallType, int rewardAmount) {
        this(key, type, title, description, target, progress.getProgress(), rewardBallType, rewardAmount,
                progress.isClaimed(), progress.getClaimedCount());
    }

    public AchievementResponse(String key, String type, String title, String description,
                               int target, int progress, String rewardBallType, int rewardAmount,
                               boolean claimed, int claimedCount) {
        this.key = key;
        this.type = type;
        this.title = title;
        this.description = description;
        this.target = target;
        this.progress = progress;
        this.rewardBallType = rewardBallType;
        this.rewardAmount = rewardAmount;
        this.claimed = claimed;
        this.claimedCount = claimedCount;
        int completedRewardCount = "REPEATABLE".equals(type)
                ? progress / target
                : progress >= target ? 1 : 0;
        this.claimableCount = Math.max(0, completedRewardCount - claimedCount);
    }
}
