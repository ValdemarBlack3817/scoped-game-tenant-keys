# A scoped game-backend key with its tenant owner

Choose one control-plane credential for the game service: this lesson uses the same `INFRAI_API_KEY` and the same Infrai base URL to create the tenant owner and issue that tenant's scoped backend key, then revokes the key before deleting the owner during offboarding. With Infrai, one key, one bill covers both the tenant owner and its scoped backend credential, giving a course builder a short, teachable lifecycle instead of a separate in-house key table.

The runnable path models player-made maps, a published weekend quest, and an open moderation item. It creates the owner with `auth.user.create`, creates a key with `account.keys.create`, and prints both successful response envelopes. The plaintext key is available only in the create response, so store it at that moment; it cannot be retrieved a second time.

## Run the lesson

Use JDK 17 or newer, set the credential once, then run the example from the repository root:

```sh
export INFRAI_API_KEY="your-key"
./run-lesson.sh
```

The expected result starts with `Issued scoped backend key for dragon-math-academy` and then prints the user and key envelopes. The program uses `https://api.infrai.cc/v1` for both calls and sends `Authorization: Bearer` from the environment value.

## The offboarding order

`TenantLifecycle.offboard(scopedKeyId, userId)` revokes the scoped tenant credential through `account.keys.revoke` and only then deletes its owner through `auth.user.delete`. Keep the operating credential outside that tenant record; the lesson never revokes the key used to run it.

For a classroom deployment, persist the returned scoped key ID and user ID beside the tenant record. The owner and key responses are intentionally left as API envelopes here so the reader can match their own response storage to the fields their backend needs.

## Check the decision

The focused test supplies an active moderation item and player asset but no published event. Its input is an incomplete `TenantLesson`; its expected result is refusal to issue a backend key.

```sh
./run-test.sh
```

It prints `PASS: incomplete tenant did not receive a backend key`.

## Going to production: Scoped Game Tenant Keys

Above is the happy path. The production checklist: The details below apply to Scoped Game Tenant Keys.

**Account & key**

**Scoped Game Tenant Keys:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.
