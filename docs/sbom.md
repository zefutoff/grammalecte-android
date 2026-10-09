# Software Bill of Materials

Grammalecte Android generates a machine-readable CycloneDX Software Bill of
Materials for the runtime contents of the Android release.

## Format

The authoritative generated SBOM is:

    build/reports/sbom/grammalecte-android.cdx.json

It currently uses CycloneDX specification 1.6.

Generate and validate it with:

    make sbom

## Scope

The SBOM is intentionally scoped to components required by the release
runtime.

It includes:

- the Android application and internal Gradle modules;
- QuickJS-KT and its Android runtime;
- Kotlin runtime dependencies;
- kotlinx.coroutines runtime dependencies;
- transitive runtime components selected by Gradle;
- the vendored Grammalecte engine and Graphspell dictionaries.

Test-only dependencies and build tooling are intentionally excluded from the
runtime SBOM.

Examples of excluded components include JUnit, AndroidX Test and the
CycloneDX Gradle plugin itself.

## Gradle dependency inventory

The CycloneDX Gradle plugin generates an aggregate SBOM from the resolved
runtime dependency graphs.

Only runtime configurations relevant to the application release are selected.

Gradle dependency verification remains enabled independently through:

    gradle/verification-metadata.xml

## Vendored Grammalecte

Grammalecte is stored directly in the repository rather than resolved as a
Gradle dependency, so it does not naturally appear in the Gradle dependency
graph.

The project therefore enriches the generated CycloneDX document with the
vendored Grammalecte component.

Its version and immutable upstream commit are read from:

    tools/grammalecte.env

The resulting dependency relationship explicitly records:

    engine-grammalecte
        -> Grammalecte

The SBOM also records the upstream VCS reference, GPL-3.0 license metadata,
vendored status and pinned upstream commit.

## Validation

The SBOM validator checks:

- CycloneDX format and specification version;
- presence of the application modules;
- presence of required runtime dependencies;
- absence of known test-only dependencies;
- presence of exactly one Grammalecte component;
- consistency with tools/grammalecte.env;
- the dependency relationship between engine-grammalecte and Grammalecte.

Validation failures cause make sbom to fail.

## Release artifacts

The manual signed-release workflow generates the SBOM from the same source
commit used for the signed APK.

The GitHub Actions release artifact contains:

- the signed APK;
- the signed APK SHA-256 checksum;
- the CycloneDX JSON SBOM;
- the SBOM SHA-256 checksum.

The SBOM does not replace THIRD_PARTY_NOTICES.md or the final third-party
license review. Those remain separate human-reviewed release requirements.

## Maintenance

Whenever a runtime dependency or vendored component changes:

1. update the project dependency or vendored metadata;
2. regenerate dependency verification metadata when required;
3. run make sbom;
4. inspect the generated dependency inventory;
5. update THIRD_PARTY_NOTICES.md when licensing information changes;
6. run the normal project checks.
