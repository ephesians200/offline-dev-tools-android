# 280+ Offline Dev Tools

An offline toolbox for programmers, built for Android with Kotlin and Jetpack Compose. More than 200 tools and 400 actions in 18 categories run entirely on the device, so your code, tokens and keys are never uploaded.

[![Get it on Google Play](https://img.shields.io/badge/Google%20Play-Download-3DDC84?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.codingtools.app)

## What it does

| Category | Examples |
|---|---|
| Formatters | JSON, XML, YAML, TOML, HTML, CSS, SQL |
| Converters | JSON to YAML/XML/CSV, JSON to classes in 10 languages, cURL to code, number bases, units |
| Encoders | Base64/32/58/85, URL, JWT decode and verify, hex, QR codes and barcodes |
| Crypto | Hashes, HMAC, AES-GCM, ChaCha20, RSA/EC keys, bcrypt, scrypt, Argon2, TOTP, password generator |
| Generators | UUID v1/4/5/7, ULID, NanoID, fake data, `.gitignore`, Dockerfile and project scaffolds |
| Text and Data | Diff, regex tester and explainer, JSONPath, jq subset, XPath, SQLite playground |
| Web and Network | Colour tools, CSS generators, CSP/CORS helpers, CIDR calculator, REST and GraphQL client, DNS, ping, port check |
| Code | Secret scanner, complexity and duplicate checks, stack-trace and log viewers, snippet manager, JavaScript and SQL runner |
| Low-level | Hex dump, IEEE-754, Protobuf/MessagePack/CBOR decoders, ASN.1, file inspector |
| Reference and Learn | Cheat sheets, Big-O, HTTP and port tables, sorting visualiser, flashcards |

Other features: favourites and recents, global search, a command palette, a pipeline builder that chains tools, and a secure notes vault encrypted with the Android Keystore (biometric or screen-lock gate).

## Privacy by design
- No account, no backend, no analytics.
- Input typed into hashing and security tools is never saved.
- Sensitive text copied to the clipboard is flagged as sensitive.
- The network is only used by the REST client, DNS, ping and port tools, and only when you press the button.

## How it is built
- **Language and UI:** Kotlin, Jetpack Compose, Material 3, Navigation Compose
- **Data-driven tools:** each tool is one `tool(...)` entry (id, title, category, modes, sample input). Adding an entry makes it appear in search, categories, favourites and the pipeline builder.
- **Libraries:** `zxing-core` (QR/barcodes), `bcprov` (BLAKE2, SHA-3, bcrypt, scrypt, Argon2)
- **Monetisation:** a one-time Pro unlock with Google Play Billing, plus an AdMob banner shown with Google's consent flow (UMP) for free users
- **Tests:** unit tests for the tokenizer, the tool registry, the Pro tool list and the regex guard

```
app/src/main/java/com/codingtools/app
├── MainActivity.kt
├── data/      Store.kt (favourites, recents, prefs), Vault.kt (Keystore AES-GCM)
├── ui/        Theme, Components, AppRoot, ToolScreen, custom screens, Paywall, Onboarding
├── billing/   BillingManager.kt
├── ads/       Ads.kt (consent + AdMob)
└── tools/     Pure-Kotlin tool logic
```

## Build it yourself
1. Open the folder in Android Studio (JDK 17).
2. Let Gradle sync, then run on a device or emulator (Android 8.0 / API 26 or newer).
3. The AdMob ids can be overridden with `ADMOB_APP_ID` and `ADMOB_BANNER_ID` in your own `gradle.properties`.

## Author
**Prince Oppong**, software and web developer and tech educator.
[Portfolio](https://princeoppong.safeoddsfootballtips.com/) · [LinkedIn](https://www.linkedin.com/in/ohenebaoppong/)

The bundled JetBrains Mono font is licensed under the SIL Open Font License (see `app/FONT-LICENSE-JetBrainsMono-OFL.txt`).
