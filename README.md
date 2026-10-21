# Kontakt

Kontakt is an Android VK client under active development.

## Source of truth

The current Android project is stored in [`kontakt-project-fixed-repository.zip`](./kontakt-project-fixed-repository.zip). The GitHub Actions workflow extracts this archive into a temporary workspace before building. Keep this archive in version control until the Android source is migrated into normal tracked directories; do not treat generated APK artifacts as source code.

## Build and download a test APK

1. Open [GitHub Actions](../../actions/workflows/build.yml).
2. Select **Build Kontakt APK** and open the latest successful run.
3. Download the `Kontakt-debug-apk` artifact from the run's **Artifacts** section.

Workflow runs can also be started manually from the Actions page. Debug APK artifacts are configured to be retained for 90 days; they are temporary build outputs, not permanent releases.

## Development workflow

- Make changes on a feature branch and review them through a pull request.
- A successful CI build verifies that the configured project compiles and its configured unit-test task passes; it does **not** prove VK ID sign-in works on a physical device.
- Never commit API secrets, signing keys, or real user tokens.
- Before publishing a user-facing build, verify VK ID login on-device and review the manifest/configuration for secrets.

## Current limitations

The Android source is still packaged in a ZIP rather than tracked as ordinary source files. Migrating it into normal directories is a priority because it will make code review, edits by multiple contributors, and targeted diffs much safer and easier. No production release is implied by the presence of a successful debug build.
