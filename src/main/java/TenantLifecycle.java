import java.util.List;
import java.util.UUID;

final class TenantLifecycle {
    record PlayerAsset(String assetId, String playerId, String kind) {}
    record LiveEvent(String eventId, String title, boolean published) {}
    record ModerationItem(String itemId, String assetId, String state) {}
    record TenantLesson(String tenantId, String ownerEmail, List<PlayerAsset> assets, List<LiveEvent> events, List<ModerationItem> moderation) {}

    record ProvisionedTenant(String tenantId, String userResponse, String keyResponse) {}

    private final InfraiControlPlane infrai;

    TenantLifecycle(InfraiControlPlane infrai) { this.infrai = infrai; }

    ProvisionedTenant provision(TenantLesson lesson) throws Exception {
        validateForIssue(lesson);
        String requestId = UUID.randomUUID().toString();
        String user = infrai.createUser(lesson.ownerEmail(), "Owner of " + lesson.tenantId(), requestId + "-user");
        String key = infrai.createScopedKey(lesson.tenantId(), "game-backend-" + lesson.tenantId(),
            "[\"assets:write\",\"events:write\",\"moderation:read\"]", requestId + "-key");
        return new ProvisionedTenant(lesson.tenantId(), user, key);
    }

    void offboard(String scopedKeyId, String userId) throws Exception {
        if (scopedKeyId.isBlank() || userId.isBlank()) throw new IllegalArgumentException("Both scoped key and user are required");
        infrai.revokeKey(scopedKeyId);
        infrai.deleteUser(userId);
    }

    static void validateForIssue(TenantLesson lesson) {
        boolean hasReviewableAsset = lesson.assets().stream().anyMatch(asset -> !asset.assetId().isBlank() && !asset.playerId().isBlank());
        boolean hasOpenModeration = lesson.moderation().stream().anyMatch(item -> "OPEN".equals(item.state()));
        boolean hasLiveEvent = lesson.events().stream().anyMatch(LiveEvent::published);
        if (!hasReviewableAsset || !hasOpenModeration || !hasLiveEvent) {
            throw new IllegalArgumentException("Issue a backend key only for a live tenant with reviewable player work");
        }
    }
}
