import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

let loadedContext = null;

const fakeSpellChecker = {
  isValid(word) {
    return word === "correct";
  },
  parseParagraph() {
    return [
      { nStart: 4, nEnd: 11, sValue: "serveur" }
    ];
  },
  suggest(word, limit) {
    if (limit === 0) return [];
    if (word === "serveur") return [["serveurs", "serveur"], ["service"]];
    return [["corrigé"]];
  }
};

globalThis.gc_engine = {
  load(context, colorType, path) {
    loadedContext = { context, colorType, path };
  },
  parse() {
    return [
      {
        nStart: 17,
        nEnd: 26,
        aSuggestions: ["installés", "installées", "installés"],
        sMessage: "Accord incorrect",
        sRuleId: "TEST_RULE"
      }
    ];
  },
  getSpellChecker() {
    return fakeSpellChecker;
  }
};

const source = fs.readFileSync(
  new URL("../engine-grammalecte/src/main/assets/android_bridge.js", import.meta.url),
  "utf8"
);
vm.runInThisContext(source, { filename: "android_bridge.js" });

globalThis.__grammalecteAndroid.init();
assert.deepEqual(loadedContext, {
  context: "JavaScript",
  colorType: "aRGB",
  path: ""
});

const issues = JSON.parse(
  globalThis.__grammalecteAndroid.check(
    "Les serveur sont installer",
    "fr-FR"
  )
);

assert.equal(issues.length, 2);
assert.deepEqual(issues[0], {
  start: 17,
  end: 26,
  suggestions: ["installés", "installées"],
  message: "Accord incorrect",
  ruleId: "TEST_RULE",
  kind: "GRAMMAR"
});
assert.deepEqual(issues[1], {
  start: 4,
  end: 11,
  suggestions: ["serveurs", "serveur", "service"],
  message: "",
  ruleId: "SPELLING",
  kind: "SPELLING"
});

assert.deepEqual(
  JSON.parse(globalThis.__grammalecteAndroid.checkWord("correct", "fr-FR", 5)),
  { valid: true, suggestions: [] }
);
assert.deepEqual(
  JSON.parse(globalThis.__grammalecteAndroid.checkWord("fote", "fr-FR", 1)),
  { valid: false, suggestions: ["corrigé"] }
);
assert.deepEqual(
  JSON.parse(globalThis.__grammalecteAndroid.checkWord("fote", "fr-FR", 0)),
  { valid: false, suggestions: [] }
);

console.log("JavaScript bridge contract OK");
