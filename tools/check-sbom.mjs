#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDirectory, "..");

const sbomPath =
    process.argv[2] ??
    path.join(
        root,
        "build/reports/sbom/grammalecte-android.cdx.json",
    );

const grammalecteMetadataPath =
    path.join(root, "tools/grammalecte.env");

function parseEnvironmentFile(file) {
    const values = {};

    for (const rawLine of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
        const line = rawLine.trim();

        if (!line || line.startsWith("#")) {
            continue;
        }

        const separator = line.indexOf("=");

        if (separator === -1) {
            fail("invalid metadata line: " + line);
        }

        values[line.slice(0, separator).trim()] =
            line.slice(separator + 1).trim();
    }

    return values;
}

function fail(message) {
    console.error("SBOM validation failed: " + message);
    process.exit(1);
}

if (!fs.existsSync(sbomPath)) {
    fail("file not found: " + sbomPath);
}

const bom = JSON.parse(fs.readFileSync(sbomPath, "utf8"));

if (bom.bomFormat !== "CycloneDX") {
    fail("unexpected bomFormat: " + bom.bomFormat);
}

if (bom.specVersion !== "1.6") {
    fail("unexpected CycloneDX specification: " + bom.specVersion);
}

if (!Array.isArray(bom.components)) {
    fail("components array is missing");
}

if (!Array.isArray(bom.dependencies)) {
    fail("dependencies array is missing");
}

function componentsNamed(name) {
    return bom.components.filter(
        (component) => component.name === name,
    );
}

function requireSingleComponent(name) {
    const matches = componentsNamed(name);

    if (matches.length !== 1) {
        fail(
            "expected exactly one component named " +
                name +
                ", found " +
                matches.length,
        );
    }

    return matches[0];
}

const app = requireSingleComponent("app");
const core = requireSingleComponent("core");
const engine = requireSingleComponent("engine-grammalecte");
const spellchecker = requireSingleComponent("spellchecker");
const grammalecte = requireSingleComponent("grammalecte");

for (const component of [
    app,
    core,
    engine,
    spellchecker,
    grammalecte,
]) {
    if (!component["bom-ref"]) {
        fail(component.name + " has no bom-ref");
    }
}

if (!fs.existsSync(grammalecteMetadataPath)) {
    fail(
        "Grammalecte metadata not found: " +
            grammalecteMetadataPath,
    );
}

const grammalecteMetadata =
    parseEnvironmentFile(grammalecteMetadataPath);

const expectedGrammalecteVersion =
    grammalecteMetadata.GRAMMALECTE_VERSION;

const expectedGrammalecteCommit =
    grammalecteMetadata.GRAMMALECTE_COMMIT;

if (!expectedGrammalecteVersion || !expectedGrammalecteCommit) {
    fail("incomplete Grammalecte metadata");
}

if (grammalecte.version !== expectedGrammalecteVersion) {
    fail(
        "unexpected Grammalecte version: " +
            grammalecte.version,
    );
}

const expectedGrammalecteRef =
    "pkg:github/algoo/grammalecte@" +
    expectedGrammalecteCommit;

if (grammalecte["bom-ref"] !== expectedGrammalecteRef) {
    fail(
        "unexpected Grammalecte bom-ref: " +
            grammalecte["bom-ref"],
    );
}

const engineDependency = bom.dependencies.find(
    (dependency) => dependency.ref === engine["bom-ref"],
);

if (!engineDependency) {
    fail("engine-grammalecte dependency relation is missing");
}

if (
    !engineDependency.dependsOn?.includes(
        expectedGrammalecteRef,
    )
) {
    fail(
        "engine-grammalecte does not depend on vendored Grammalecte",
    );
}

const requiredRuntimeComponents = [
    "quickjs-kt",
    "quickjs-kt-android",
    "kotlin-stdlib",
    "kotlinx-coroutines-core",
];

for (const name of requiredRuntimeComponents) {
    if (componentsNamed(name).length === 0) {
        fail("required runtime component missing: " + name);
    }
}

const forbiddenTestComponents = [
    "junit",
    "json",
    "core",
    "runner",
    "rules",
    "ext-junit",
];

for (const component of bom.components) {
    const group = String(component.group ?? "");
    const name = String(component.name ?? "");

    if (group.startsWith("androidx.test")) {
        fail(
            "test-only AndroidX dependency present: " +
                group +
                ":" +
                name,
        );
    }

    if (
        group === "junit" &&
        forbiddenTestComponents.includes(name)
    ) {
        fail(
            "test-only JUnit dependency present: " +
                group +
                ":" +
                name,
        );
    }

    if (group === "org.json" && name === "json") {
        fail("test-only org.json dependency present");
    }
}

console.log("SBOM validation OK.");
console.log("CycloneDX specification: " + bom.specVersion);
console.log("Components: " + bom.components.length);
console.log(
    "Vendored Grammalecte: " +
        grammalecte.version +
        " (" +
        grammalecte["bom-ref"] +
        ")",
);
