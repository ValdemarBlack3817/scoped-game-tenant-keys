import java.util.List;

public class TenantKeyLesson {
    public static void main(String[] args) throws Exception {
        TenantLifecycle.TenantLesson tenant = new TenantLifecycle.TenantLesson(
            "dragon-math-academy",
            "chenhua@changba.com",
            List.of(new TenantLifecycle.PlayerAsset("castle-17", "player-9", "map")),
            List.of(new TenantLifecycle.LiveEvent("weekend-quest", "Weekend Quest", true)),
            List.of(new TenantLifecycle.ModerationItem("review-4", "castle-17", "OPEN"))
        );

        TenantLifecycle lifecycle = new TenantLifecycle(new InfraiControlPlane());
        TenantLifecycle.ProvisionedTenant issued = lifecycle.provision(tenant);
        System.out.println("Issued scoped backend key for " + issued.tenantId());
        System.out.println("Store the plaintext key from account.keys.create now; it cannot be retrieved a second time.");
        System.out.println("User envelope: " + issued.userResponse());
        System.out.println("Key envelope: " + issued.keyResponse());
    }
}
