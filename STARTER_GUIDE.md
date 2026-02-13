# Mindlog Android Starter Guide

## Scope (Phase 1)
- Turbo Native shell
- Browser-based social login + deep-link return
- WebView session exchange (`/auth/exchange`)

## Build Flavors
- `dev`: `https://127.0.0.1:8443` (use `adb reverse tcp:8443 tcp:8443`)
- `prod`: `https://www.mindlog.blog`

## Key Rules
- `BuildConfig.BASE_URL` is the single base URL source.
- Route behavior is driven by `app/src/main/assets/json/path-configuration.json`.
- Custom route handlers are minimized; deep-link handling remains in `MainActivity`.

## Run / Test
```bash
adb reverse tcp:8443 tcp:8443
./gradlew assembleDevDebug
./gradlew installDevDebug
./gradlew testDevDebugUnitTest
```

## Login Flow
1. App navigates to `/auth/login?source=app`.
2. Path configuration opens login externally.
3. Browser returns `mindlog://auth/callback?token=...`.
4. App routes to `${BASE_URL}/auth/exchange?token=...`.
5. WebView session is established.

## Troubleshooting
- If local HTTPS fails, verify backend cert setup (`mindlog/scripts/setup-local-https-cert.sh`).
- If app shows only endless spinner on `dev`, verify reverse tunnel with `adb reverse --list`.
- If login opens in WebView, re-check `open_external` rule for `/auth/login.*`.
