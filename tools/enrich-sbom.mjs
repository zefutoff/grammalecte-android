#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDirectory, "..");

const input =
    process.argv[2] ??
    path.join(root, "build/reports/cyclonedx/bom.json");

const output =
    process.argv[3] ??
    path.join(root, "build/reports/sbom/grammalecte-android.cdx.json");

const metadataFile = path.join(root, "tools/grammalecte.env");

function fail(message) {
    console.error(message);
    process.exit(1);
}

function parseEnvironmentFile(file) {
    const values = {};

    for (const rawLine of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
        const line = rawLine.trim();

        if (!line || line.startsWith("#")) {
            continue;
        }

        const separator = line.indexOf("=");

        if (separator === -1) {
            fail("Invalid metadata line: " + line);
        }

        const key = line.slice(0, separator).trim();
        const value = line.slice(separator + 1).trim();

        values[key] = value;
    }

    return values;
}

function requireValue(values, name) {
    const value = values[name];

    if (!value) {
        fail("Missing Grammalecte metadata: " + name);
    }

    return value;
}

if (!fs.existsSync(input)) {
    fail("CycloneDX input not found: " + input);
}

if (!fs.existsSync(metadataFile)) {
    fail("Grammalecte metadata not found: " + metadataFile);
}

const metadata = parseEnvironmentFile(metadataFile);

const version = requireValue(metadata, "GRAMMALECTE_VERSION");
const commit = requireValue(metadata, "GRAMMALECTE_COMMIT");
const repository = requireValue(
    metadata,
    "GRAMMALECTE_REPOSITORY",
).replace(/\.git$/, "");

const bom = JSON.parse(fs.readFileSync(input, "utf8"));

if (!Array.isArray(bom.components)) {
    fail("CycloneDX document has no components array.");
}

if (!Array.isArray(bom.dependencies)) {
    fail("CycloneDX document has no dependencies array.");
}

const engineComponents = bom.components.filter(
    (component) => component.name === "engine-grammalecte",
);

if (engineComponents.length !== 1) {
    fail(
        "Expected exactly one engine-grammalecte component, found: " +
            engineComponents.length,
    );
}

const engineRef = engineComponents[0]["bom-ref"];

if (!engineRef) {
    fail("engine-grammalecte component has no bom-ref.");
}

const grammalecteRef =
    "pkg:github/algoo/grammalecte@" + commit;

const grammalecteComponent = {
    type: "library",
    group: "algoo",
    name: "grammalecte",
    version,
    scope: "required",
    "bom-ref": grammalecteRef,
    purl: grammalecteRef,
    description:
        "Vendored French spelling and grammar engine and Graphspell dictionaries.",
    licenses: [
        {
            license: {
                name: "GPL-3.0",
            },
        },
    ],
    externalReferences: [
        {
            type: "vcs",
            url: repository + "/tree/" + commit,
        },
    ],
    properties: [
        {
            name: "grammalecte-android:vendored",
            value: "true",
        },
        {
            name: "grammalecte-android:upstream-commit",
            value: commit,
        },
    ],
};

bom.components = bom.components.filter(
    (component) => component["bom-ref"] !== grammalecteRef,
);

bom.components.push(grammalecteComponent);

bom.components.sort((left, right) =>
    String(left["bom-ref"] ?? "").localeCompare(
        String(right["bom-ref"] ?? ""),
    ),
);

const engineDependency = bom.dependencies.find(
    (dependency) => dependency.ref === engineRef,
);

if (!engineDependency) {
    fail("Dependency entry for engine-grammalecte was not found.");
}

engineDependency.dependsOn = Array.from(
    new Set([
        ...(engineDependency.dependsOn ?? []),
        grammalecteRef,
    ]),
).sort();

if (!bom.dependencies.some(
    (dependency) => dependency.ref === grammalecteRef,
)) {
    bom.dependencies.push({
        ref: grammalecteRef,
        dependsOn: [],
    });
}

bom.dependencies.sort((left, right) =>
    String(left.ref ?? "").localeCompare(
        String(right.ref ?? ""),
    ),
);

fs.mkdirSync(path.dirname(output), {
    recursive: true,
});

fs.writeFileSync(
    output,
    JSON.stringify(bom, null, 2) + "\n",
);

console.log("Enriched CycloneDX SBOM written to:");
console.log(output);
console.log();
console.log("Vendored component:");
console.log("  Grammalecte " + version);
console.log("  " + commit);
console.log("  parent: engine-grammalecte");
