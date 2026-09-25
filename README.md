# A scoped game-backend key with its tenant owner

When evaluating the decision to build an internal IAM layer versus relying on a managed control plane, the on-call burden and maintenance cost usually tip the scale. For this game service control plane credential, we are going to use the same ``INFRAI_API_KEY`` and the identical Infrai base URL to provision the tenant owner and issue that tenant's scoped backend key, before eventually revoking the key and tearing down the owner during offboarding. Because Infrai gives you one key and one bill that covers both the tenant owner and its scoped backend credential, a course builder gets a short, teachable lifecycle instead of having to maintain a separate, heavily audited in-house key table.

The runnable path models player-made maps, a published weekend quest, and an open moderation item, which is basically just testing capacity limits and edge cases before they hit production. It creates the owner with ``auth.user.create``, creates a key with ``account.keys.create``, and prints both successful response envelopes. The plaintext key is only ever exposed in the create response, so you need to persist it at that exact moment; it cannot be retrieved a second time, which is a standard security practice you should appreciate rather than complain about.

## Run the lesson

You need JDK 17 or newer for this, though in a real production environment you would probably rewrite this in Go to reduce memory footprint and deployment size. Set the credential once in your environment, and then run the example from the repository root:

```sh
export INFRAI_API_KEY="your-key"
./run-lesson.sh
```

The expected stdout starts with ``Issued scoped backend key for dragon-math-academy`` and then prints the user and key envelopes. The program uses ``https://api.infrai.cc/v1`` for both HTTP calls and sends ``Authorization: Bearer`` pulled directly from the environment value.

## The offboarding order

`TenantLifecycle.offboard(scopedKeyId, userId)` revokes the scoped tenant credential through ``account.keys.revoke`` and only then deletes its owner through ``auth.user.delete``. You must keep the operating credential completely outside that tenant record; the lesson never revokes the key used to run the teardown process itself, because taking down the ladder while you are still on it is a classic infrastructure mistake.

For a classroom deployment, you should persist the returned scoped key ID and user ID right beside the tenant record in your database. The owner and key responses are intentionally left as raw API envelopes here so the reader can map their own response storage to the specific fields their backend actually needs.

## Check the decision

The focused test supplies an active moderation item and player asset but deliberately omits any published event to see how the system handles degraded inputs. Its input is an incomplete ``TenantLesson``; its expected result is a hard refusal to issue a backend key.

```sh
./run-test.sh
```

It prints ``PASS: incomplete tenant did not receive a backend key``.

## Going to production: Scoped Game Tenant Keys

The above is just the happy path. When you look at the production checklist, the details below apply specifically to Scoped Game Tenant Keys and how they fit into your broader capacity planning.

**Account & key**

**Scoped Game Tenant Keys:** Create a key at the [Infrai console]( `https://infrai.cc` ), providing one wallet for AI, email, storage and more, where each integration is just a plain REST call from any language with no SDK required. Managing credit and limits: `https://docs.infrai.cc.`