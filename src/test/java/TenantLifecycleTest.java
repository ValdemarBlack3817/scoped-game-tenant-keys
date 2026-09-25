import java.util.List;

public class TenantLifecycleTest {
    public static void main(String[] args) {
        TenantLifecycle.TenantLesson incomplete = new TenantLifecycle.TenantLesson(
            "practice-tenant", "teacher@example.test",
            List.of(new TenantLifecycle.PlayerAsset("asset-1", "player-1", "skin")),
            List.of(new TenantLifecycle.LiveEvent("event-1", "Practice", false)),
            List.of(new TenantLifecycle.ModerationItem("queue-1", "asset-1", "OPEN"))
        );
        try {
            TenantLifecycle.validateForIssue(incomplete);
            throw new AssertionError("A key must not be issued before a live event exists");
        } catch (IllegalArgumentException expected) {
            System.out.println("PASS: incomplete tenant did not receive a backend key");
        }
    }
}
