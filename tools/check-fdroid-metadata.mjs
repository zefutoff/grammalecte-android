#!/usr/bin/env node

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(scriptDirectory, "..");

const locales = ["en-US", "fr-FR"];

const requirements = {
    "title.txt": 50,
    "short_description.txt": 80,
    "full_description.txt": 4000,
};

function fail(message) {
    console.error("F-Droid metadata validation failed: " + message);
    process.exit(1);
}

for (const locale of locales) {
    const directory = path.join(
        root,
        "fastlane",
        "metadata",
        "android",
        locale,
    );

    for (const [fileName, maximumLength] of Object.entries(
        requirements,
    )) {
        const file = path.join(directory, fileName);

        if (!fs.existsSync(file)) {
            fail(locale + ": missing " + fileName);
        }

        const content = fs.readFileSync(file, "utf8").trim();

        if (!content) {
            fail(locale + ": empty " + fileName);
        }

        const length = [...content].length;

        if (length > maximumLength) {
            fail(
                locale +
                    ": " +
                    fileName +
                    " is " +
                    length +
                    " characters; maximum is " +
                    maximumLength,
            );
        }

        console.log(
            locale +
                " " +
                fileName +
                ": " +
                length +
                "/" +
                maximumLength,
        );
    }
}

console.log("F-Droid metadata validation OK.");
