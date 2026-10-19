# Using Monogram MTProto

How to call the library: credentials, sign-in, live updates, sending,
downloads, scheduling, proxies, and errors, with recipes you can adapt. For
build instructions see the [README](README.md); for how it works inside see
[ARCHITECTURE.md](ARCHITECTURE.md).

## Contents

- [Conventions](#conventions)
- [A complete session](#a-complete-session)
- [Credentials](#credentials)
- [Client lifecycle](#client-lifecycle)
- [Signing in](#signing-in)
- [Reading chats and history](#reading-chats-and-history)
- [Live updates](#live-updates)
- [Sending messages](#sending-messages)
- [Downloads](#downloads)
- [Scheduling](#scheduling)
- [Cancellation](#cancellation)
- [Proxies and transport](#proxies-and-transport)
- [Errors](#errors)
- [Security and privacy](#security-and-privacy)
- [Other languages](#other-languages)
- [Diagnostics](#diagnostics)
- [FAQ](#faq)
- [API index](#api-index)
- [Checklist](#checklist)
- [Glossary](#glossary)
- [References](#references)

## Conventions

- Examples are Kotlin. Rust uses snake_case names and `u64` handles; Kotlin uses
  camelCase and `ULong`.
- The API is blocking. Every call blocks the calling thread, so call it from a
  worker thread, never from a UI thread.
- A client is a `u64` handle, not an object. Nothing closes it automatically;
  call `destroyClient`.
- Errors are `MtprotoException` in Kotlin and `MtprotoError` in Rust.
- Some behavior depends on thread-local state: the dispatch class and the bound
  request-control token. Set both on the thread that makes the call.
- Code in the recipes that is not a library function (`applyEvents`,
  `savePendingMessage`, `freshRandomId`, and so on) stands for your own code.

## A complete session

This is the whole path from nothing to a running client. The sections after it
explain each step.

```kotlin
// 1. Describe your app to Telegram. Sent once per new socket.
setInitConnectionInfo(
    InitConnectionInfo(
        deviceModel = "Pixel 8",
        systemVersion = "Android 15",
        appVersion = "1.0",
        systemLangCode = "en",
        langPack = "android",
        langCode = "en",
    )
)

// 2. Create the client. key32 is 32 random bytes that you keep in a keystore.
val handle: ULong = createEncryptedClient(apiId, apiHash, sessionPath, key32)

// 3. Optional: configure before connecting.
setDownloadConcurrency(2, 4)
// setClientTestDc(handle, true)   // test datacenters only
// setProxy("socks5", host, port, user, pass, ByteArray(0))

connect(handle)

// 4. Sign in if there is no stored authorization.
if (!isAuthorized(handle)) {
    val sent = sendAuthCode(handle, phone)
    // Show a prompt that matches sent.codeType and sent.codeLength.
    try {
        signIn(handle, phone, sent.phoneCodeHash, code)
    } catch (e: MtprotoException.PasswordRequired) {
        checkPassword(handle, password)
    }
}

// 5. Start updates and run the drain loop on a background thread (see Live updates).
startUpdates(handle)

// 6. Load the first screen.
val chats = getChats(handle)
val page = getHistoryPage(handle, chats.first().id, 50, 0, 0, 0)

// 7. Send, with a pending row kept until the server confirms.
val randomId = Random.nextLong(1, Long.MAX_VALUE)
savePendingMessage(randomId, draft)
sendTextMessage(handle, chats.first().id, draft.text, 0, null, 0, null, randomId)

// 8. On exit.
destroyClient(handle)          // keeps the authorization
// logout(handle)              // revokes it on the server
```

## Credentials

### Sign-in only

The library signs in existing accounts only. It has no `auth.signUp`, and
Telegram can reject sign-up from third-party apps with
`PHONE_NUMBER_APP_SIGNUP_FORBIDDEN`. Create the account in an official Telegram
app first.

`auth.authorizationSignUpRequired`, `PHONE_NUMBER_UNOCCUPIED`, and
`SIGN_UP_REQUIRED` all surface as `MtprotoError::RegistrationRequired`.
Supporting sign-up would be a new API, not a missing flag. See
[Sign in/sign up](https://core.telegram.org/api/auth#sign-in-sign-up).

### Creating an API ID

1. Log in to [my.telegram.org](https://my.telegram.org) with your phone number.
2. Open "API development tools" and fill in the form.
3. Copy `api_id` and `api_hash`.

Notes:

- One phone number can have one `api_id`. Telegram sends developer notices to
  that number, so keep it active.
- Sample API IDs from Telegram's source are rate-limited and produce
  `API_ID_PUBLISHED_FLOOD`. Do not use them in a released app.
- The ID identifies your application and the hash is sent with `auth.sendCode`.
  Keep both out of public repositories, logs, and bug reports.
- Follow the [API Terms of Service](https://core.telegram.org/api/terms). The
  app must say it uses the Telegram API and must not imply it is official.

> [!WARNING]
> my.telegram.org applies server-side anti-abuse checks. It can refuse API ID
> creation from a VPN, proxy, datacenter or Tor address, or a flagged IP, usually
> with a generic form error. No MTProto parameter changes this. Retry later from
> a regular connection. The procedure is documented in
> [Creating your Telegram Application](https://core.telegram.org/api/obtaining_api_id);
> the IP check itself is not a documented rule.

### API ID troubleshooting

| You see | Likely cause | Fix |
| --- | --- | --- |
| `API_ID_INVALID` | Wrong `api_id`/`api_hash` pair, or values from different apps | Copy both from the same app |
| `API_ID_PUBLISHED_FLOOD` | Sample or published API ID | Use your own private ID; wait if you hit the limit |
| Generic error while creating the app | VPN, proxy, datacenter, or flagged IP; repeated submissions | Use a regular connection and try later |
| Login fails for every number | Empty or wrong credentials in your config | Check what you pass to `createClient` |
| Login works with a test number only | Test datacenter flag still set | Remove `setClientTestDc` for production |

Many apps show a generic message for the two API ID errors. When login fails
without a useful message, log the sanitized error type before suspecting the
phone number.

## Client lifecycle

### Create a client

```kotlin
setInitConnectionInfo(InitConnectionInfo(/* see A complete session */))

val handle: ULong = createClient(apiId, apiHash, sessionPath)
connect(handle)
```

The `initConnection` values are sent when a fresh socket is created, wrapped
around the first request together with the TL layer. Blank fields fall back to
defaults. Set them before the first `connect`.

A handle owns one session file and one authorization. Do not open the same
session from two handles or two processes. Telegram answers a second main
connection on the same authorization with `AUTH_KEY_DUPLICATED`, and the session
is then marked dead.

Other calls:

- `isAuthorized(handle)` checks the stored authorization
- `setClientTestDc(handle, true)` switches to test datacenters; call it before
  `connect`
- `logout(handle)` revokes the authorization on the server and clears local
  state
- `destroyClient(handle)` closes sockets and flushes the session. It does not
  revoke the authorization. Using the handle afterwards throws
  `MtprotoException.UnknownClient`

### Encrypted sessions

`createClient` writes the session as plaintext JSON.
`createEncryptedClient(apiId, apiHash, sessionPath, key)` takes exactly 32 random
bytes and encrypts the same document with AES-256-GCM.

What the session file contains: the auth snapshot, user id, peers, update
cursors, the channel recovery queue, the media index, the update journal, and
imported keys for other datacenters. Writes are atomic.

Rules for the key:

- Generate it once with a secure random source and store it in the platform
  keystore. The library does not store or wrap it.
- Pass the same 32 bytes on every launch. Losing the key means losing the local
  session; sign in again.
- Zero your copy of the key after the call returns.
- A wrong key or a damaged file fails the restore and leaves the stored session
  untouched, so a bad key cannot destroy a good session.
- A plaintext session is migrated to encrypted on the first encrypted restore.
  Going the other way is not supported.

Example for Android, where the data key is wrapped by a non-exportable Keystore
key and saved next to the session (this wrapper is app code, not part of the
library):

```kotlin
fun createClient(apiId: Int, apiHash: String, sessionPath: String): ULong {
    val key = SessionKeyStore.loadOrCreate(sessionPath)   // 32 bytes, unwrapped
    return try {
        createEncryptedClient(apiId, apiHash, File(sessionPath).canonicalPath, key)
    } finally {
        key.fill(0)
    }
}
```

A backup of the session file cannot be restored on another device when the
wrapping key is device-bound. That is intended. Outside Android, protect the key
yourself, and use plaintext sessions only in tests.

### Connect and authorization state

`connect(handle)` opens the transport and prepares the session. It does not
create an account. Use `isAuthorized` or try `sendAuthCode` to find out whether
the user has to sign in.

A connection error can be retried. A `session invalidated` error cannot: the
authorization is gone and the user must sign in again.

### Destroy versus logout

`logout` is the user-facing "log out" action. It revokes the authorization on
the server and clears local state. `destroyClient` is process teardown: it closes
sockets and flushes the session but leaves the authorization alive, so the next
launch restores it without a login.

## Signing in

### Send a code

```kotlin
val sent = sendAuthCode(handle, phone)
// sent.phone, sent.phoneCodeHash, sent.codeType, sent.codeLength
```

`phone` is the number in international format. Keep `sent.phoneCodeHash`; every
later step needs it. `resendAuthCode(handle, phone, sent.phoneCodeHash)` asks for
another delivery attempt, often through a different channel.

### Code types

Telegram chooses how the code is delivered. Show the returned `codeType` instead
of assuming SMS. Details:
[Code types](https://core.telegram.org/api/auth#code-types).

| `codeType` | Meaning | What to tell the user |
| --- | --- | --- |
| `app` | Service message in another logged-in Telegram session | Open the official Telegram app |
| `sms`, `sms_word`, `sms_phrase` | SMS, including word/phrase variants | Check the phone; wait before resending |
| `call`, `flash_call`, `missed_call` | Voice or missed call | Answer, or read the caller number as instructed |
| `email` | Code sent to the login email | Check that inbox |
| `email_setup` | Telegram requires a login email | Set it up in an official app, then retry |
| `fragment` | Code available through Fragment | Use an official app. `AuthCodeSent` has no Fragment URL field |
| `firebase` | Firebase SMS flow | Official mobile apps only; resend or pick another method |

`codeLength` tells you how many digits to accept in the input field. If you build
your own login screen, handle every type in the table explicitly.

### Submit the code and password

```kotlin
try {
    val signedIn = signIn(handle, phone, sent.phoneCodeHash, code)
    // signedIn.userId, signedIn.dcId
} catch (e: MtprotoException.PasswordRequired) {
    // ask for the cloud password, then call checkPassword
} catch (e: MtprotoException.RegistrationRequired) {
    // no account for this number, or Telegram rejected app sign-up
}
```

`checkPassword(handle, password)` runs the SRP exchange and returns
`AuthSignedIn`. The password is used locally for the exchange and is not logged.
Errors you will see there:

- `PASSWORD_HASH_INVALID`: wrong password
- `SRP_ID_INVALID`: the challenge expired; ask for the password again
- `AUTH_RESTART`: start over with a new code
- `SESSION_PASSWORD_NEEDED`: 2FA is required (the same signal as
  `PasswordRequired`, seen in failure paths)

Spec: [Two-factor authentication](https://core.telegram.org/api/srp).

A login state machine for your UI usually has these states:

| State | Entered when | Leaves when |
| --- | --- | --- |
| Phone | Nothing is authorized | `sendAuthCode` succeeds |
| Code | `AuthCodeSent` returned | `signIn` succeeds, `PasswordRequired` is thrown, or the code expires |
| Password | `PasswordRequired` | `checkPassword` succeeds |
| Done | `signIn` or `checkPassword` returned | You call `startUpdates` |

Return to the Phone state on `AUTH_RESTART`, `PHONE_CODE_EXPIRED`, or
`PHONE_NUMBER_INVALID`.

### No code or no SMS

A custom client is not guaranteed an SMS. When no code arrives:

1. Check `codeType`. If it is `app`, the code is in an official session that is
   already logged in.
2. `firebase`: resend, request the next type, or sign in through an official app.
   Third-party apps cannot complete this flow.
3. `fragment`: use an official app. The DTO exposes type and length only.
4. `email` or `email_setup`: check the login email, or set it up in an official
   app.
5. `PHONE_NUMBER_FLOOD`: wait. Resending faster makes it worse.
6. `SEND_CODE_UNAVAILABLE`: every delivery option for this attempt was used.
   Resend later or use the suggested next type.
7. New number: create the account in an official app first.
8. Try another network if the current one is blocked.

Developers whose app really needs SMS can ask Telegram to enable it by emailing
`sms@telegram.org` with `#enableSMS` in the subject. The client cannot switch it
on.

### Test datacenters

Call `setClientTestDc(handle, true)` before `connect`. Test accounts are
separate from production accounts, and a build that uses them must never ship as
production.

## Reading chats and history

```kotlin
val chats = getChats(handle)                      // dialogs, also indexes avatars
val page = getHistoryPage(handle, chatId, 50, 0, 0, 0)
```

`getHistoryPage(handle, chatId, limit, offsetId, offsetDate, addOffset)` follows
Telegram's [offset rules](https://core.telegram.org/api/offsets). `limit` is
clamped to 1..100. To load older messages, pass the id of the oldest message you
have as `offsetId`.

These calls run on the read lane. Set a read dispatch class on the calling thread:
`0` for something the user is waiting for, `1` for prefetching. See
[Scheduling](#scheduling).

Reading also fills the media index that downloads depend on: `getChats` indexes
dialog avatars and last-message media, `getHistoryPage` indexes message media, and
`getProfile` indexes a peer's avatar and profile media. Fetch the chat or history
first, then download.

`peerId` values come from `ChatDto.id`, `MessageDto.senderId`, or `ProfileDto.id`.
Do not build or re-encode channel ids yourself. The crate encodes users as
positive, basic groups as negative, and channels below -1000000000000.

## Live updates

### The model

Telegram delivers events as a stream and numbers them so that a client can tell
whether it missed something. Three counters matter (see
[Working with Updates](https://core.telegram.org/api/updates)):

| Counter | What it orders |
| --- | --- |
| `pts` | Events in one message box: the common box for private chats and basic groups, plus one box per channel or supergroup |
| `qts` | A secondary box for secret chats and certain bot events |
| `seq` | Whole update packets on the top level |

For each box the rule is the same: if the local counter plus the event count
equals the new counter, apply it; if it is already past, drop the duplicate; if
there is a hole, recover the gap with `updates.getDifference` or
`updates.getChannelDifference`.

You do not implement this. The library applies the rules, recovers gaps, and
turns the result into batches. Your job is to apply batches to your own storage
and acknowledge them.

### Drain loop

Updates are polled. Call `startUpdates` once, then run a drain loop on a
background thread with the background-read dispatch class:

```kotlin
setDispatchClass(1)

while (running) {
    val drain = try {
        drainUpdates(handle)
    } catch (e: MtprotoException) {
        Thread.sleep(2000)
        continue
    }

    if (drain.replayRequired) {
        for (batch in pendingUpdateBatches(handle)) {
            applyEvents(batch.events)
            acknowledgeUpdateBatch(handle, batch.accountGeneration, batch.batchId)
        }
        Thread.sleep(25)
        continue
    }

    for (batch in drain.batches) {
        applyEvents(batch.events)
        acknowledgeUpdateBatch(handle, batch.accountGeneration, batch.batchId)
    }
    applyEvents(drain.liveEvents)
    Thread.sleep(2000)
}
```

`drainUpdates` returns nothing until the client is authorized and
`startUpdates` has been called. The two-second sleep is a polling interval you
can tune; the replay branch sleeps briefly so a backlog drains quickly.

### Journal and acknowledgements

- `batches` are durable and already stored in the session journal. Apply the
  events, commit them to your storage, then acknowledge.
- `liveEvents` (typing, presence, sync edges) are not durable and not journaled.
- Delivery is at-least-once. A batch stays in the journal until you acknowledge
  it, so a crash replays it. Event handling must be idempotent.
- While `replayRequired` is true, unacknowledged batches exist and live events
  are held back until you apply them.
- The journal is capped at 256 unacknowledged batches or 8 MiB. A consumer that
  never acknowledges eventually stalls the pipeline.

Acknowledge only after your own state is committed. Acknowledging first loses
events on a crash.

Making event handling idempotent is usually easy:

- Messages: use the message id as the primary key for the common box, and
  `(peerId, messageId)` for channels. Upsert instead of insert.
- Deletes: deleting something that is already gone is a no-op.
- Reads and reactions: store the latest value, not a delta.

### Events

`UpdateEventDto` covers new, edited, and deleted messages, typing, presence and
emoji status, read receipts, reactions, discussion reads, dialog patches, folder
and chat changes, saved GIFs, and `SyncState`.

Constructors the library does not map arrive as `Ignored(kind)`. Match on the
kind string if you need one of them. One of them,
`UpdateMessageId:<randomId>:<messageId>`, confirms a pending send.

`SyncState` marks edges of recovery work. You can use it to show a "Updating..."
indicator while the library catches up after being offline.

### Lazy sync

`updateLazySyncConfig(handle, lazy, exceptions)` keeps gaps in off-screen
channels durable without recovering them right away. Chat ids in `exceptions`
stay eager. Use it to avoid downloading the backlog of hundreds of channels the
user is not looking at, while keeping the chat on screen fully live.

### Recipe: pending sends and confirmation

An outgoing message has three moments: you created it, the server accepted it,
and you saw its id. The `random_id` ties them together, and Telegram stores used
random ids permanently, so a repeated send with the same id returns the original
message instead of creating a duplicate. This is why the id must be fresh per
logical message and kept until the confirmation arrives.

```kotlin
fun send(chatId: Long, text: String) {
    val randomId = Random.nextLong(1, Long.MAX_VALUE)
    savePendingMessage(randomId, chatId, text)          // 1. visible, "sending"
    try {
        sendTextMessage(handle, chatId, text, 0, null, 0, null, randomId)
        // the returned MessageDto already has the server id
        markSent(randomId)
    } catch (e: MtprotoException) {
        // 2. unknown outcome: keep the row; the confirmation may still arrive
    }
}

fun onEvent(e: UpdateEventDto) {
    if (e is UpdateEventDto.Ignored && e.kind.startsWith("UpdateMessageId:")) {
        val (_, randomId, messageId) = e.kind.split(":")
        markSent(randomId.toLong(), messageId.toInt())  // 3. confirmed
    }
}
```

If the response to a send is lost (a dropped session, an internal server error),
the confirmation still arrives through updates and your pending row resolves
correctly. Do not resend with a new random id just because a call failed.

### Recipe: surviving process death

Because of the journal, the order "apply, commit, acknowledge" is the whole
contract. After a crash the next `drainUpdates` returns `replayRequired` and
`pendingUpdateBatches` hands you the unacknowledged batches again, and your
idempotent handlers bring storage to the same state. You do not need to track
update counters yourself.

## Sending messages

### Text

Give each send a fresh, nonzero random `Long`:

```kotlin
val randomId = Random.nextLong(1, Long.MAX_VALUE)
savePendingMessage(randomId, draft)

val message = sendTextMessage(
    handle = handle,
    chatId = chatId,
    text = draft.text,
    replyToMsgId = 0,
    entitiesJson = null,
    topMsgId = 0,
    webpageUrl = null,
    clientRandomId = randomId,
)
```

- `replyToMsgId` is the message being replied to, or `0`.
- `entitiesJson` is a compact JSON array of `{"kind","offset","length","url?"}`
  objects. Offsets and lengths follow Telegram's
  [entity rules](https://core.telegram.org/api/entities) (UTF-16 code units).
- `topMsgId` is the forum topic or thread root.
- `webpageUrl` attaches a preview for an explicit URL.

Keep the pending message in your own storage until
`UpdateEventDto.Ignored("UpdateMessageId:<randomId>:<id>")` arrives. The library
does not store drafts. Passing `0` as the random id makes the library generate
one, but then you cannot match the send to its confirmation.

### Photos

```kotlin
val photo = sendPhotoMessage(
    handle, chatId, path, caption,
    replyToMsgId = 0, topMsgId = 0, entitiesJson = null,
)
```

The photo is uploaded and sent in one call. This helper takes no client random
id, so a lost response cannot be matched; use `sendUploadedMedia` when you need
the pending-row flow.

### Documents, video, and albums

```kotlin
val item = UploadItemDto(
    path = file.absolutePath,
    kind = "video",
    mimeType = "video/mp4",
    fileName = "clip.mp4",
    caption = "",
    entitiesJson = null,
    duration = 12,
    width = 1080,
    height = 1920,
    randomId = freshRandomId(),
)
val sent = sendUploadedMedia(handle, chatId, item, 0, 0, null)
val album = sendUploadedAlbum(handle, chatId, items, 0, 0)
```

An album holds at most ten items, and photos or videos cannot be mixed with
documents. Each item carries its own `randomId`. Files over 10 MiB use
`upload.saveBigFilePart`. Photos over 10 MiB are rejected.

### How uploads behave

- Uploads run on four fixed upload lanes, separate from reads and downloads, so a
  large upload does not delay chat loading.
- Part size is 32 KiB or 512 KiB (`setFilePartKib`: below 512 gives 32 KiB, 512
  and above gives 512 KiB). On a slow link (round trip of a second or more) or
  for a tiny file, one 32 KiB part goes at a time; otherwise up to eight parts are
  in flight.
- Uploads honor cancellation tokens like any other call. See
  [Cancellation](#cancellation).

### Edit, delete, forward, and other actions

`editTextMessage`, `deleteMessage`, `forwardMessages`, `readHistory`,
`readMessageContents`, `setTyping`, `sendReaction`, and
`getMessageReactionsList` are in the [API index](#api-index). They take the
client handle first and use the same dispatch-class and error rules as the calls
shown above. Use the interactive-write class (`4`) for actions the user just
performed.

## Downloads

All download calls block until the file is complete or fails, and return the
destination path.

| Function | Kind | Result |
| --- | --- | --- |
| `downloadMessageMedia(handle, chatId, messageId, destPath)` | Full | Original photo, video, or document |
| `downloadMessageThumb(handle, chatId, messageId, destPath)` | Thumb | Smallest preview; may use inline bytes with no network |
| `downloadMessageDisplay(handle, chatId, messageId, destPath)` | Display | Sized photo variant |
| `downloadMessageMediaChunk(handle, chatId, messageId, destPath, offset)` | Full from offset | Bytes from `offset`, written from byte 0 of `destPath` |

`destPath` is a file path, not a directory. Missing parent directories are
created. A network download replaces the destination only when it is published,
so a failed download never destroys the previous file. Spec:
[Files](https://core.telegram.org/api/files).

### Dispatch class

Downloads run on media lanes, so set a media dispatch class on the calling
thread first:

```kotlin
setDispatchClass(6) // visible media
val path = downloadMessageMedia(handle, chatId, messageId, destPath)
```

Valid classes are 2 (interactive media), 3 (background media), 5 (ordinary
media), 6 (visible media), and 7 (user media). Classes 0 and 1 are read classes;
a full download with one of them fails with `read class on the media family`.
The class also picks the download profile. See [Scheduling](#scheduling).

### Picking a class

| Situation | Class | Why |
| --- | --- | --- |
| Avatar in a list the user is scrolling | 2 | Small and urgent; the user sees it right now |
| Photo in the chat that is on screen | 6 | Visible media: large chunks, high parallelism |
| User tapped "download" or opened a file | 7 | User-initiated: highest throughput |
| Thumbnails for items near the viewport | 5 | Ordinary: steady, modest |
| Prefetch, auto-download on Wi-Fi, cache warming | 3 | Background: gentle, ages upward if it waits |

### Download profiles

| Class | Profile (Full) | Chunk | Parts in flight |
| --- | --- | --- | --- |
| 2 interactive media | User | 512 KiB | 8 |
| 3 background media | Background | 128 KiB | 4 |
| 5 ordinary media | Ordinary | 128 KiB | 4 |
| 6 visible media | Visible | 512 KiB | 8 |
| 7 user media | User | 512 KiB | 8 |

- Thumb always uses 128 KiB and one part.
- Full with an offset always uses the Playing profile: 512 KiB, up to 8 parts.
- Display uses Visible when the class sets no profile.
- `setDownloadChunkKib` accepts 128, 256, or 512 and falls back to 128 for
  anything else. It does not change the number of parts in flight.

Telegram requires file request offsets and limits to be multiples of 4 KiB and
a request to stay inside one 1 MiB block. The chunk sizes above all divide 1 MiB.
Spec: [Optimization](https://core.telegram.org/api/optimisation).

### Media index

Downloads read from the library's media index. These calls fill it:

- `getChats` indexes dialog avatars and last-message media
- `getHistoryPage(handle, chatId, limit, offsetId, offsetDate, addOffset)`
  indexes message media; `limit` is clamped to 1..100
- `getProfile` indexes the peer's avatar and profile media

If a message entry is missing, the download re-fetches its file source and looks
back up to three messages. Avatar entries trigger a profile or dialog refresh.
If the entry is still missing you get `no media for chat <chatId> message
<messageId>`. Fetch the dialog, profile, or history first, then retry.

The index is bounded: 500 message entries, 200 entries for stickers, GIFs, inline
files, and custom emoji, 32 instant-view entries, and unlimited peer avatars.
Very old messages fall out of the persisted index, so re-read history before
downloading media from far back.

### Resume and cancel

A full download writes to `<dest>.<pid>.<sequence>.part`, with two sidecar
files next to it:

- `...ranges.part` records the missing byte ranges
- `...identity.part` records file id, access hash, DC, size, and version

On success the staging file is renamed to `destPath`. Retryable failures
(timeouts, flood waits, network loss, file references) and cancellation keep the
staging file and sidecars. The next call with the same `destPath` resumes from
the largest staging file if the identity matches, and starts over if it does
not. Other errors delete the staging file. Stale staging files older than two
minutes are swept, and the oldest are deleted first if the total passes 32 MiB.

Offset downloads are not resumed. `offset` must be non-negative and a multiple
of 128 KiB, or the call fails with `invalid media range offset`.

To cancel from another thread, write any content to `destPath + ".cancel"`. The
marker is checked between parts and before publishing. The library never
deletes it, so remove it before the next download to the same path.

Concurrent downloads to the same `destPath` run one after the other under a
per-path lock. Finished files are not deduplicated, so check for an existing
file yourself before starting.

### Progress

```kotlin
setDownloadProgressListener(object : DownloadProgressListener {
    override fun onProgress(path: String, downloaded: Long, total: Long) {
        // runs on the native download thread; hop to your UI thread
    }
})
```

The listener is process-wide, so one callback serves every download; route by
`path`. `downloaded` starts at the resume offset, and `total` is usually 0
because the final size is not always known. Use `MessageDto.fileSize` when you
have it.

### Recipe: image grid

A scrolling grid wants something on screen fast and sharpness later:

1. For every visible cell, call `peekMessageInlineThumb` first. It does no
   network I/O, so it is safe on the render path.
2. If it returns null, call `downloadMessageThumb` with class `5`.
3. When a cell settles on screen, call `downloadMessageDisplay` with class `6`.
4. Only on tap, call `downloadMessageMedia` with class `7`.
5. When a cell scrolls away, cancel its pending request with a request-control
   token (see [Cancellation](#cancellation)) so it stops competing for lanes.

### Recipe: streaming a video

`downloadMessageMediaChunk(handle, chatId, messageId, destPath, offset)` returns
bytes from `offset` onward and writes them from byte 0 of `destPath`. Use it to
serve a player: pick a 128 KiB-aligned `offset` for the position the player
requests, download into a scratch file, and feed the player from it. This path
uses the high-throughput Playing profile and is not resumed, so keep a separate
full download going at class `3` if you also want the file cached.

### Recipe: a background cache warmer

```kotlin
setDispatchClass(3)                 // background media
for (item in queue) {
    if (isCached(item)) continue
    try {
        downloadMessageMedia(handle, item.chatId, item.messageId, item.path)
    } catch (e: MtprotoException) {
        if (isFloodWait(e)) { Thread.sleep(waitSeconds(e) * 1000L) } else continue
    }
}
```

Class `3` uses small chunks and four parts, and ages toward interactive priority
while it waits, so it never starves behind a steady stream of visible media but
also never crowds it out.

### Tuning downloads

`setDownloadConcurrency(lanes, parts)` sets media lanes and parts in flight.
Both are clamped to 1..8; the defaults are 2 and 4. `downloadConcurrency()`
returns `[lanes, parts]`. More lanes mean more TCP sessions, and media on a
foreign DC needs temporary keys too, so raise them only when the device and
network can use them.

| Goal | Change |
| --- | --- |
| Faster large files on a fast link | Higher parts (6 or 8), chunk 512 KiB, use class 6 or 7 |
| Several files at once | More lanes (3 or 4) |
| Lower memory and socket use on weak devices | Fewer lanes and parts, chunk 128 KiB |
| Smoother UI during bulk work | Use class 3 for bulk and keep visible items on 6 |
| Flood waits keep appearing | Reduce lanes and parts; the library also halves its own window |

Admission is limited per datacenter: by default five small files and two large
ones (20 MB or more) at once, or whatever `help.getAppConfig` says. Queued work
ages. While a DC is recovering from a flood wait and still has active transfers,
new ones wait.

Measure before tuning. `perfSnapshot` reports lane waits and first-chunk timing
(see [Diagnostics](#diagnostics)).

### CDN and file references

CDN redirects are followed automatically. Each part is decrypted and checked
against the master DC's SHA-256 hashes, and any failure falls back to the origin
DC, so a download never accepts unverified CDN data. Spec:
[CDN](https://core.telegram.org/cdn#verifying-files).

`FILE_REFERENCE` errors re-fetch the message, sticker set, wallpaper, or inline
query that produced the media and retry once. A thumbnail tries the next
getFile-sized thumbnail first. `FILE_MIGRATE_X` only changes the media DC, not
your account session, and is followed automatically. Spec:
[File references](https://core.telegram.org/api/file-references).

### Download errors

| Message | Cause |
| --- | --- |
| `no media for chat ...` | Media not indexed yet, or peer/message not cached |
| `no downloadable thumb` | No usable thumbnail size |
| `no display size` | No display-size variant |
| `invalid media range offset` | Offset negative or not chunk-aligned |
| `cancelled` / `request cancelled` | Cancel marker or request-control token |
| `read class on the media family` | No media dispatch class set |

A flood error that reaches you means the request was rejected. Schedule it again
after the wait the server gave.

### Avatars and other media

Peer photos are indexed at `(peerId, 0)` and downloaded with `messageId = 0`:

```kotlin
setDispatchClass(2)

getProfile(handle, peerId)
val avatarPath = downloadMessageMedia(handle, peerId, 0, destPath)
val inlineJpeg = peekPeerInlineThumb(handle, peerId) // ByteArray? or null
```

`peekPeerInlineThumb` returns an expanded in-memory JPEG or null. It does no
network I/O and does not take the main lane lock, so it is safe to call while
rendering; it only holds the client data mutex briefly. `peekMessageInlineThumb`
is the message version.

If `(peerId, 0)` is missing, the download asks for the profile or refreshes the
dialogs before failing. After that, `no media for chat` means the peer has no
photo or is not cached.

For a sized still use `downloadMessageDisplay(handle, peerId, 0, path)`. Video
avatars are indexed as `video_avatar`: a full download fetches the video, while
the thumb stays a still image. The library upgrades a peer photo to the video
variant when the profile reports one.

More media entry points:

- `downloadCustomEmoji(handle, documentId, destPath)` for a custom emoji indexed
  at `(documentId, 0)`
- `downloadWallpaper(handle, id, accessHash, destPath)` for a wallpaper from
  `getWallpapers`

## Scheduling

### Lanes

| Family | Count | Used for |
| --- | --- | --- |
| Main | 1 | RPCs that must see updates, plus the update connection |
| Read | up to 2 extra | History, dialogs, profiles, other `invokeWithoutUpdates` reads |
| Media | 2 by default, up to 8 | File downloads and wallpapers |
| Upload | 4 | Outgoing file parts |

Extra read lanes exist only when the server allows parallel sessions
(`config.tmp_sessions`) and a temporary key is bound. Otherwise reads use the
main lane. Media lanes open as needed up to the configured count. Upload lanes
are fixed at four.

Keep large history loads and downloads off the main lane. `getHistoryPage` and
profile calls use the read lane; downloads use media lanes. The dispatch class
must match the lane family of the call. Spec:
[Invoking methods](https://core.telegram.org/api/invoking),
[Parallel sessions](https://core.telegram.org/api/datacenter#parallel-sessions).

### Dispatch classes

`setDispatchClass` sets a thread-local value that selects the lane family and
the waiting priority. A fresh thread starts at 0.

| Value | Name | Family | Priority |
| --- | --- | --- | --- |
| 0 | interactive read | read | 1 |
| 1 | background read | read | 3 |
| 2 | interactive media | media | 2 |
| 3 | background media | media | 4, ages to 1 |
| 4 | interactive write | read | 0 |
| 5 | ordinary media | media | 2 |
| 6 | visible media | media | 2 |
| 7 | user media | media | 2 |

Lower numbers go first, FIFO within a priority. Classes with a priority above 2
move up one step every 5 seconds, down to 1. A background request can therefore
eventually pass interactive media (2) but never an interactive write (0).

Set the class right before each blocking call and restore it afterwards in a
`finally`. In coroutine code, set it from a thread-context element so it follows
the coroutine onto its worker thread:

```kotlin
class DispatchClass(private val value: Int) : ThreadContextElement<Unit> {
    companion object Key : CoroutineContext.Key<DispatchClass>
    override val key get() = Key
    override fun updateThreadContext(context: CoroutineContext) { setDispatchClass(value) }
    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) { setDispatchClass(0) }
}

withContext(Dispatchers.IO + DispatchClass(6)) {
    downloadMessageMedia(handle, chatId, messageId, destPath)
}
```

`promoteRequestControl(token, class)` raises the priority of a request that is
already queued on the read lanes, for example when an item the user scrolled
past comes back on screen. It does not change the download profile. Media-lane
waits and download admission use the calling thread's class, so set that before
starting a download.

### Settings

- `setDownloadConcurrency(lanes, parts)`: 1..8 each
- `setDownloadChunkKib(kib)`: 128, 256, or 512; anything else becomes 128
- `setFilePartKib(kib)`: upload part size; below 512 gives 32 KiB, 512 and above
  gives 512 KiB
- `downloadConcurrency()`, `downloadChunkKib()`: current values

## Cancellation

```kotlin
val token = createRequestControl()
val previous = bindRequestControl(token)
try {
    val page = getHistoryPage(handle, chatId, 50, 0, 0, 0)
} finally {
    bindRequestControl(previous)
    releaseRequestControl(token)
}

// from another thread
cancelRequestControl(token)
```

- `createRequestControl` allocates a token.
- `bindRequestControl` attaches it to the calling thread and returns the
  previous token, which you restore in `finally`.
- `cancelRequestControl` closes the sockets registered to the token and makes
  later checks fail. It does not wait for the worker to return.
- `releaseRequestControl` frees the token.

A token works once: after cancellation, calls that use it fail with `request
cancelled`. Create a new token for the next request. Reusing a released token is
not supported.

Cancellation is checked while a request waits in the queue, so a cancelled
request that never got a lane exits immediately.

Downloads can also be cancelled with the `.cancel` marker, which needs no token.

### Recipe: cancellable suspend wrapper

```kotlin
suspend fun <T> nativeCall(dispatchClass: Int, block: () -> T): T =
    suspendCancellableCoroutine { cont ->
        val token = createRequestControl()
        val worker = executor.submit {
            val previous = bindRequestControl(token)
            setDispatchClass(dispatchClass)
            try {
                cont.resume(block())
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            } finally {
                setDispatchClass(0)
                bindRequestControl(previous)
                releaseRequestControl(token)
            }
        }
        cont.invokeOnCancellation {
            cancelRequestControl(token)
            worker.cancel(true)
        }
    }
```

This is the pattern the Monogram Android bridge uses, reduced to its essentials:
bind a token and a class on the worker thread, cancel the token and the worker
together when the coroutine is cancelled.

## Proxies and transport

```kotlin
setProxy("socks5", "proxy.example.com", 1080, "user", "pass", ByteArray(0))
setProxy("http", "proxy.example.com", 8080, null, null, ByteArray(0))
setProxy("https", "proxy.example.com", 443, null, null, ByteArray(0))

val latencyMs = pingProxy("socks5", "proxy.example.com", 1080, null, null, ByteArray(0))
clearProxy()
```

Supported kinds: `socks5`, `http`, `https` (TLS plus CONNECT), and `mtproto`.
`pingProxy` times a throwaway connection and does not change the active proxy.
`clearProxy` goes back to direct connections.

MTProxy secrets are raw bytes:

- simple: the 16-byte key
- `dd`: `0xdd` followed by the 16-byte key (17 bytes)
- `ee`: `0xee`, the 16-byte key, then the ASCII SNI domain

```kotlin
fun ddSecret(key: ByteArray) = byteArrayOf(0xdd.toByte()) + key
fun eeSecret(key: ByteArray, domain: String) =
    byteArrayOf(0xee.toByte()) + key + domain.toByteArray(Charsets.US_ASCII)

setProxy("mtproto", "proxy.example.com", 443, null, null, eeSecret(key, "example.com"))
```

Fake TLS in `ee` mode hides the traffic and is authenticated with the proxy
secret. It is not certificate-verified TLS.

`setTransportMode("padded_intermediate")` is the default;
`setTransportMode("http")` selects the HTTP transport. Spec:
[MTProto transports](https://core.telegram.org/mtproto/mtproto-transports).

HTTPS proxy certificates are checked with `rustls-platform-verifier`. On
Android, call the JNI function `initPlatformVerifier(context)` once before
using an HTTPS proxy.

### Recipe: choosing among several proxies

```kotlin
val best = proxies
    .mapNotNull { p -> runCatching { p to pingProxy(p.kind, p.host, p.port, p.user, p.pass, p.secret) }.getOrNull() }
    .minByOrNull { it.second }
    ?.first
if (best != null) setProxy(best.kind, best.host, best.port, best.user, best.pass, best.secret)
```

Apply the proxy before `connect`, or reconnect after changing it. When a network
is blocked, direct connections rotate datacenter ports (443 first) on their own;
a proxy is for networks where that is not enough.

## Errors

### Error shape

Server failures arrive as `MtprotoException.Message` in one of these forms:

- `RPC <code>: <TYPE>`, for example `RPC 420: FLOOD_WAIT_5`
- `session invalidated: <REASON>`, for a session the library already knows is
  dead. It is deliberately not reported as a fake 401, so logs show the real
  reason.
- Local or transport text: `RPC timeout`, `connection error`, `msg_key`,
  `bad_msg_notification`, or an unknown constructor

The official references are [Error handling](https://core.telegram.org/api/errors)
and the machine-readable
[errors.json](https://core.telegram.org/api/errors.json): `errors` maps code to
error type to the methods that can emit it, `descriptions` holds readable text,
and durations use printf-style `%d` placeholders. The library returns the raw
strings and leaves classification to you. Monogram's Android bridge classifies
them with a Kotlin catalog generated from `errors.json`.

### A small classifier

```kotlin
sealed interface Failure {
    data class Flood(val seconds: Int) : Failure
    object SessionDead : Failure
    object Network : Failure
    data class Rpc(val code: Int, val type: String) : Failure
    data class Other(val text: String) : Failure
}

fun classify(text: String): Failure {
    if (text.startsWith("session invalidated")) return Failure.SessionDead
    Regex("""RPC (\d+): (\w+)""").find(text)?.let { m ->
        val code = m.groupValues[1].toInt()
        val type = m.groupValues[2]
        Regex("""FLOOD(?:_PREMIUM)?_WAIT_(\d+)""").find(type)?.let {
            return Failure.Flood(it.groupValues[1].toInt())
        }
        if (type in setOf("AUTH_KEY_UNREGISTERED", "AUTH_KEY_INVALID", "SESSION_REVOKED",
                          "SESSION_EXPIRED", "AUTH_KEY_DUPLICATED")) return Failure.SessionDead
        return Failure.Rpc(code, type)
    }
    if (text.startsWith("RPC timeout") || text.startsWith("connection error")) return Failure.Network
    return Failure.Other(text)
}
```

Match on the type, not on message substrings, and keep the raw type for bug
reports.

### Common errors

| Error | Meaning | What to do |
| --- | --- | --- |
| `API_ID_INVALID` | Invalid `api_id`/`api_hash` pair | Fix your credentials |
| `API_ID_PUBLISHED_FLOOD` | Published API ID, rate-limited | Use your own private ID |
| `PHONE_NUMBER_APP_SIGNUP_FORBIDDEN` | Sign-up through this app is not allowed | Create the account in an official app |
| `PHONE_NUMBER_UNOCCUPIED` | No account for this number | Same as above |
| `PHONE_NUMBER_BANNED` | Number is banned | Do not retry |
| `PHONE_NUMBER_FLOOD` | Too many code requests | Wait; do not resend repeatedly |
| `PHONE_NUMBER_INVALID` | Invalid phone number | Fix the number or format |
| `SEND_CODE_UNAVAILABLE` | All code delivery options used | Resend later or use the next type |
| `PHONE_CODE_INVALID` / `PHONE_CODE_EXPIRED` | Wrong or expired code | Re-enter or resend |
| `SESSION_PASSWORD_NEEDED` | 2FA is enabled | Ask for the password, call `checkPassword` |
| `PASSWORD_HASH_INVALID` | Wrong cloud password | Ask again |
| `SRP_ID_INVALID` | SRP challenge expired | Restart password entry |
| `AUTH_RESTART` | Restart the sign-in flow | Send a new code |
| `AUTH_KEY_UNREGISTERED`, `AUTH_KEY_INVALID`, `SESSION_REVOKED`, `SESSION_EXPIRED` | Authorization is dead | Sign in again |
| `AUTH_KEY_DUPLICATED` | The same authorization is used by parallel connections | Run one main session per authorization; sign in again |
| `AUTH_KEY_PERM_EMPTY` | Temporary key not bound to the permanent key | Retry; not a logout |
| `AUTH_KEY_UNSYNCHRONIZED` | Key state mismatch | Retry; not a logout |
| `USER_DEACTIVATED` | Account deleted or deactivated | Stop and tell the user |
| `PHONE_MIGRATE_X`, `NETWORK_MIGRATE_X`, `USER_MIGRATE_X`, `FILE_MIGRATE_X` | Another DC owns the phone, network, user, or file | Handled inside the library |
| `FILE_REFERENCE_*` | Expired or invalid file reference | Handled inside downloads: re-fetch the source and retry once |
| `FILE_ID_INVALID` | Invalid file id | Refresh the message or media index |
| `FILE_PART_INVALID` / `FILE_PARTS_INVALID` | Bad upload part metadata | Fix the upload parameters |
| `FLOOD_WAIT_X` | Wait X seconds | Retry after the wait; show long waits to the user |
| `FLOOD_PREMIUM_WAIT_X` | Same, with a Premium alternative | Respect the wait or offer Premium |
| `Timeout` / `MSG_WAIT_TIMEOUT` | Request timed out | Retry reads; do not blindly retry sends |
| `RPC timeout` | Local attempt budget used up | Retry with care |
| `connection error` | Socket or transport failure | Reconnect |
| `msg_key` | Ciphertext or key mismatch | Treat the session as suspect; reconnect |
| `bad_msg_notification` | Timing or sequence problem | Recovered internally; reconnect if it persists |
| unknown constructor | The codec does not know the constructor | Protocol update needed |
| `updates queue overflow` | Local update buffer overflowed | Difference recovery is required |

### Retry policy

| Kind of call | Retry on timeout or network error | Retry on flood wait |
| --- | --- | --- |
| Read (history, dialogs, profile) | Yes, with backoff | Yes, after the wait |
| Download | Yes; it resumes from the staging file | Yes, after the wait |
| Upload part | Handled inside the library | Handled inside the library |
| Send, edit, delete, forward | Check the outcome first (pending row, confirmation) | Yes, after the wait, with the same random id |
| Sign-in steps | No, ask the user | After the wait |

### What the library handles

- DC migration, file-reference refresh, server salt and message-id recovery
- CDN failures (fallback to origin) and flood pacing for transfers
- Replay of reads and idempotent upload parts after uncertain delivery.
  Mutations that may have reached the server are returned as errors and never
  replayed automatically.

### What your code must handle

- A dead session (`session invalidated`, `AUTH_KEY_*`, `SESSION_*`): clear local
  state and ask the user to sign in again
- Flood waits: retry after the wait, and show a countdown for long ones
- `RegistrationRequired`: tell the user the account has to exist already
- `API_ID_INVALID` and `API_ID_PUBLISHED_FLOOD`: these are configuration
  problems, not a phone number problem
- Retrying sends: check whether the message arrived before sending again
- `UnknownClient`: the handle was destroyed

### Reporting an unknown error

Include the error code and type (for example `RPC 400: SOME_NEW_ERROR`), the
`libraryVersion()` output, what you were doing and whether it reproduces, and
whether a proxy or VPN was involved. Leave out phone numbers, login codes,
session files, API hashes, auth keys, and message text.

## Security and privacy

- Telegram auth keys stay inside Rust. The session data key crosses the binding
  boundary only for `createEncryptedClient`; zero your copy afterwards.
- Keep the session file and any wrapped key in protected storage. A copied
  session cannot be restored on another device when the wrapping key is
  device-bound.
- `api_id` and `api_hash` identify your app, not your users. They can be
  extracted from a shipped binary, so treat them as app credentials. Do not put
  API hashes, login codes, phone numbers, auth keys, or session files in logs or
  issue reports.
- Proxy credentials and MTProxy secrets are raw bytes. Fake TLS is camouflage, not
  certificate-verified TLS; HTTPS proxies use the platform verifier.
- Media caches can contain private content. Expose cache clearing and respect the
  user's storage choice. The library writes only to the paths you give it, plus
  staging files next to them.
- On Android, updates must be signed by the same identity. Do not ship
  debug-signed APKs as production updates.

## Other languages

The library exports plain functions, records, enums, one error type, and one
callback interface (`DownloadProgressListener`). Generate bindings from the
built library:

```console
cargo build --release

cargo run --features bindgen-cli --bin uniffi-bindgen -- \
  generate --library target/release/libmonogram_mtproto.so \
  --language swift --out-dir out/swift
```

Use `.dylib` on macOS and `.dll` on Windows. For Kotlin the Gradle task
`:native:mtproto:buildNativeMtproto` does this and writes the output to
`native/mtproto/src/main/java`. To run it by hand, pass `--language kotlin
--no-format` and point `--library` at the built `.so`. Never edit generated
bindings; after an export or DTO change, regenerate and update your wrapper.

| Language | Generator |
| --- | --- |
| Kotlin, Swift, Python, Ruby | `uniffi-bindgen --language <name>`; for Swift also `uniffi-bindgen-swift`; Ruby needs the `ffi` gem |
| C# | [uniffi-bindgen-cs](https://github.com/NordSecurity/uniffi-bindgen-cs) (community) |
| Go | [uniffi-bindgen-go](https://github.com/NordSecurity/uniffi-bindgen-go) (community) |
| Dart / Flutter | [uniffi-bindgen-dart](https://github.com/nchapman/uniffi-bindgen-dart) (community); check that it supports UniFFI 0.32 |

Generated API shape:

| Rust / UniFFI | Kotlin | Swift | Python | Ruby |
| --- | --- | --- | --- | --- |
| `u64` handle | `ULong` | `UInt64` | `int` | `Integer` |
| `Result<T, MtprotoError>` | exception | `throws` | exception | exception |
| record | data class | struct | class | generated record |
| enum | sealed class | enum | enum/class | enum/class |
| callback interface | interface | protocol | callback object | callback object |

The bindings add no async runtime, retries, or object lifetimes. Callbacks run
on the native download thread.

### What every embedder has to supply

The Kotlin wrapper does these things for you in the Monogram app. In another
language you do them yourself:

1. A worker thread for every call. UniFFI calls are synchronous.
2. Per-call dispatch class and request-control binding (thread-local state).
3. Marshalling of `DownloadProgressListener` to your UI thread.
4. Storage for the 32-byte session key, and zeroing of your copy.
5. The drain loop for updates and an acknowledgement after each committed batch.

### Examples

Python (generated names are snake_case; check the generated module for exact
names):

```python
import monogram_mtproto as mt

handle = mt.create_client(api_id, api_hash, "session.json")
mt.connect(handle)

if not mt.is_authorized(handle):
    sent = mt.send_auth_code(handle, phone)
    mt.sign_in(handle, phone, sent.phone_code_hash, input("code: "))

mt.start_updates(handle)
chats = mt.get_chats(handle)
```

Swift (argument labels follow the Rust parameter names):

```swift
let handle = createClient(apiId: apiId, apiHash: apiHash, sessionPath: path)
try connect(handle: handle)
let sent = try sendAuthCode(handle: handle, phone: phone)
```

### Targets

The repository builds for host and Android. iOS, desktop, and WASM need their own
build and a transport/TLS review (the platform verifier setup is
Android-oriented). The crate type is `cdylib`; static linking needs a
`staticlib` target. WASM is not possible as-is because the transport uses
blocking sockets, threads, and the filesystem.

Session encryption is the same everywhere. Only the Android app has Keystore
integration, so elsewhere you pass a 32-byte key to `createEncryptedClient` and
protect it yourself.

For Flutter and Dart, build the native library for every target you ship, load
the right one per platform, run the blocking API on an isolate or worker, and
forward progress callbacks to the UI isolate. `flutter_rust_bridge` is an
alternative if you prefer a different FFI model.

See [Generating bindings](https://mozilla.github.io/uniffi-rs/latest/bindings.html)
and [Swift bindings](https://mozilla.github.io/uniffi-rs/latest/swift/overview.html).

## Diagnostics

```kotlin
val version = libraryVersion()
perfSetEnabled(true)
val snapshotJson = perfSnapshot(reset = true)
```

`libraryVersion()` returns the crate version and TL layer. `perfSetEnabled` is
off by default. `perfSnapshot` returns JSON with per-operation counts, total and
maximum microseconds, a small ring of samples, and counters. It never contains
message payloads or keys.

How to use it:

1. Enable it, reproduce the slow scenario, then take a snapshot with
   `reset = true` so the next one starts clean.
2. Look at lane-wait spans first: long waits mean too few lanes or a priority
   problem, not a slow network.
3. Look at first-chunk timing for media: a large value points at datacenter
   distance, a missing temporary key, or admission queueing.
4. Compare snapshots before and after a tuning change.

Span names are fixed strings such as API calls, media first chunk, persist, and
lane waits. Your own logs should follow the same rule: error type, code, flood
wait, DC, and your operation name, never content.

## FAQ

**Can I use one client from several threads?** Yes. A handle can be used from
many threads. What you must not do is open the same session through a second
handle or process.

**Why does every call block?** The boundary stays small and binds well to many
languages. Scheduling and cancellation belong to the host, and the recipes above
show the pattern.

**Why did my download fail with `no media for chat`?** The media index does not
have the entry yet. Load the dialog, history page, or profile first, then retry.

**Why do I get `read class on the media family`?** The calling thread has a read
dispatch class (0 or 1). Set 2, 3, 5, 6, or 7 before a download.

**Can I sign users up?** No. Create accounts in an official Telegram app.

**Do I need to track `pts`, `qts`, or `seq`?** No. The library does and hands you
acknowledged batches. See [Live updates](#live-updates).

**Is CDN data trusted?** No. Every part is hash-verified against the master
datacenter, and any failure falls back to the origin.

**What happens if the process dies in the middle of a download?** The staging file
and its sidecars stay. The next call with the same destination resumes if the file
identity matches.

**Can I run it on iOS or desktop?** Not out of the box; see
[Targets](#targets).

## API index

All functions take the client handle. Kotlin names are shown.

| Task | Functions |
| --- | --- |
| Client | `createClient`, `createEncryptedClient`, `connect`, `isAuthorized`, `destroyClient` |
| Sign-in | `sendAuthCode`, `resendAuthCode`, `signIn`, `checkPassword`, `logout` |
| Dialogs | `getChats` |
| History | `getHistoryPage`, `getHistory`, `getReplies` |
| Profile | `getProfile`, `resolveUsername` |
| Search | `searchMessagesPage`, `searchGlobal`, `contactsSearch` |
| Send | `sendTextMessage`, `sendPhotoMessage`, `sendUploadedMedia`, `sendUploadedAlbum` |
| Edit, delete, forward | `editTextMessage`, `deleteMessage`, `forwardMessages` |
| Read state | `readHistory`, `readMessageContents`, `setTyping` |
| Reactions | `sendReaction`, `getMessageReactionsList` |
| Downloads | `downloadMessageMedia`, `downloadMessageThumb`, `downloadMessageDisplay`, `downloadMessageMediaChunk` |
| Inline previews | `peekMessageInlineThumb`, `peekPeerInlineThumb` |
| Other media | `downloadCustomEmoji`, `downloadWallpaper` |
| Updates | `startUpdates`, `drainUpdates`, `pendingUpdateBatches`, `acknowledgeUpdateBatch`, `updateLazySyncConfig` |
| Scheduling and settings | `setDispatchClass`, `setDownloadConcurrency`, `setDownloadChunkKib`, `setFilePartKib` |
| Cancellation | `createRequestControl`, `bindRequestControl`, `cancelRequestControl`, `releaseRequestControl`, `promoteRequestControl` |
| Proxy and transport | `setProxy`, `pingProxy`, `clearProxy`, `setTransportMode` |
| Diagnostics | `libraryVersion`, `perfSetEnabled`, `perfSnapshot` |

## Checklist

- One session file, one handle, one process
- Your own API ID, not a sample one
- The account already exists in an official app
- `startUpdates` called after sign-in and a drain loop running
- Update batches acknowledged only after your state is committed, and handlers are
  idempotent
- A fresh random id for every outgoing message, pending row kept until confirmed
- A dispatch class set before every blocking call, a media class for downloads
- Download destination is a file path; the `.cancel` marker is cleaned up
- The same 32-byte key on every launch of an encrypted session, and your copy
  zeroed
- Errors classified by type, not by substring
- Generated bindings regenerated, never hand-edited

## Glossary

| Term | Meaning |
| --- | --- |
| API ID / hash | Application credentials from my.telegram.org |
| Auth key | The long-term MTProto key of an authorization |
| Session | The session file plus the server-side authorization |
| DC | Telegram datacenter. The account DC and a file's DC can differ |
| TL layer | Version of Telegram's schema. This build is pinned to layer 231 |
| DTO | A record or enum that crosses the binding boundary |
| Dispatch class | Thread-local value (0..7) that picks the lane family and priority |
| Lane | An internal session used for one kind of request |
| Flood wait | Server-imposed delay, `FLOOD_WAIT_<seconds>` |
| File reference | Opaque file token that can expire and must be refreshed |
| Temporary key (PFS) | Short-lived key bound to the auth key, used for extra sessions |
| SRP | The challenge used for cloud-password (2FA) login |
| `pts`, `qts`, `seq` | Telegram's counters for update ordering |
| Message box | One independent event sequence: the common box or a channel box |
| Journal | The durable list of update batches awaiting acknowledgement |
| CDN | Separate file servers; downloads verify hashes and fall back to origin |

## References

| Topic | Page |
| --- | --- |
| API overview | https://core.telegram.org/api |
| API ID creation | https://core.telegram.org/api/obtaining_api_id |
| API terms | https://core.telegram.org/api/terms |
| Authorization | https://core.telegram.org/api/auth |
| Code types | https://core.telegram.org/api/auth#code-types |
| Sign in / sign up | https://core.telegram.org/api/auth#sign-in-sign-up |
| SRP / 2FA | https://core.telegram.org/api/srp |
| Errors | https://core.telegram.org/api/errors |
| Errors JSON | https://core.telegram.org/api/errors.json |
| Invoking | https://core.telegram.org/api/invoking |
| Layers | https://core.telegram.org/api/layers |
| TL schema explorer | https://schema.jppgr.am/ |
| Pinned layer 231 | https://schema.jppgr.am/layers/231.tl |
| Layer 230 -> 231 diff | https://schema.jppgr.am/diff/230-231 |
| Layer changelog feed | https://schema.jppgr.am/api/feed |
| Updates | https://core.telegram.org/api/updates |
| Files | https://core.telegram.org/api/files |
| File references | https://core.telegram.org/api/file-references |
| CDN verification | https://core.telegram.org/cdn#verifying-files |
| Datacenters | https://core.telegram.org/api/datacenter |
| Parallel sessions | https://core.telegram.org/api/datacenter#parallel-sessions |
| Optimization | https://core.telegram.org/api/optimisation |
| Config | https://core.telegram.org/api/config |
| MTProto | https://core.telegram.org/mtproto |
| MTProto description | https://core.telegram.org/mtproto/description |
| Transports | https://core.telegram.org/mtproto/mtproto-transports |
| Service messages | https://core.telegram.org/mtproto/service_messages |
| Security guidelines | https://core.telegram.org/mtproto/security_guidelines |
| Auth-key vectors | https://core.telegram.org/mtproto/samples-auth_key |
| Folders | https://core.telegram.org/api/folders |
| Offsets | https://core.telegram.org/api/offsets |

Full grouped list: [README: Telegram documentation map](README.md#telegram-documentation-map).

**Protocol and API basics:** [MTProto index](https://core.telegram.org/mtproto), [TL schema](https://core.telegram.org/schema), [MTProto description](https://core.telegram.org/mtproto/description), [transports](https://core.telegram.org/mtproto/mtproto-transports), [service messages](https://core.telegram.org/mtproto/service_messages), [security guidelines](https://core.telegram.org/mtproto/security_guidelines), [auth-key vectors](https://core.telegram.org/mtproto/samples-auth_key), [layers](https://core.telegram.org/api/layers), [invoking](https://core.telegram.org/api/invoking), [datacenters](https://core.telegram.org/api/datacenter), [config](https://core.telegram.org/api/config).

**Data model and chats:** [peers](https://core.telegram.org/api/peers), [minimum constructors](https://core.telegram.org/api/min), [entities](https://core.telegram.org/api/entities), [links](https://core.telegram.org/api/links), [channels](https://core.telegram.org/api/channel), [discussion](https://core.telegram.org/api/discussion), [forum](https://core.telegram.org/api/forum), [threads](https://core.telegram.org/api/threads), [drafts](https://core.telegram.org/api/drafts), [pinned messages](https://core.telegram.org/api/pin), [scheduled messages](https://core.telegram.org/api/scheduled-messages), [search](https://core.telegram.org/api/search), [reactions](https://core.telegram.org/api/reactions), [mentions](https://core.telegram.org/api/mentions).

**Media:** [files](https://core.telegram.org/api/files), [file references](https://core.telegram.org/api/file-references), [CDN](https://core.telegram.org/cdn#verifying-files), [optimization](https://core.telegram.org/api/optimisation), [stickers](https://core.telegram.org/api/stickers), [custom emoji](https://core.telegram.org/api/custom-emoji), [animated emoji](https://core.telegram.org/api/animated-emojis), [GIFs](https://core.telegram.org/api/gifs), [wallpapers](https://core.telegram.org/api/wallpapers), [themes](https://core.telegram.org/api/themes), [colors](https://core.telegram.org/api/colors).

**Updates and account:** [updates](https://core.telegram.org/api/updates), [push updates](https://core.telegram.org/api/push-updates), [profile](https://core.telegram.org/api/profile), [privacy](https://core.telegram.org/api/privacy), [blocking](https://core.telegram.org/api/block), [PFS](https://core.telegram.org/api/pfs), [QR login](https://core.telegram.org/api/qr-login), [passkeys](https://core.telegram.org/api/passkeys).

**Methods used by this crate:** [auth.sendCode](https://core.telegram.org/method/auth.sendCode), [auth.signIn](https://core.telegram.org/method/auth.signIn), [auth.bindTempAuthKey](https://core.telegram.org/method/auth.bindTempAuthKey), [messages.getHistory](https://core.telegram.org/method/messages.getHistory), [messages.sendMessage](https://core.telegram.org/method/messages.sendMessage), [messages.sendMedia](https://core.telegram.org/method/messages.sendMedia), [upload.getFile](https://core.telegram.org/method/upload.getFile), [updates.getDifference](https://core.telegram.org/method/updates.getDifference), [help.getCdnConfig](https://core.telegram.org/method/help.getCdnConfig), [help.getAppConfig](https://core.telegram.org/method/help.getAppConfig).
