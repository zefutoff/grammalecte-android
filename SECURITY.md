# Security policy

## Reporting a vulnerability

Please use GitHub's **private security advisory** feature for vulnerabilities that could expose typed text, bypass the offline-only design, load untrusted JavaScript or escape the expected Android service boundary.

Do not include real private text in reports. A minimal synthetic reproduction is preferred.

## Security assumptions

- Grammalecte assets are bundled at build time from an immutable upstream commit.
- No JavaScript is downloaded at runtime.
- The application does not request the `INTERNET` permission.
- Asset paths passed through the JavaScript bridge reject parent-directory traversal.
- The QuickJS runtime is memory- and evaluation-time-bounded.
