import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

let loadedContext = null;

let fakeOptions = new Map([
  ["typo", true],
  ["infi", true],
  ["idrule", false]
]);

const fakeDefaultOptions = new Map(fakeOptions);

const loadedFiles = [];
const initializedModules = {};

globalThis.helpers = {
  loadFile(path) {
    loadedFiles.push(path);
    return `DATA:${path}`;
  }
};

globalThis.conj = {
  init(data) {
    initializedModules.conj = data;
  }
};

globalThis.phonet = {
  init(data) {
    initializedModules.phonet = data;
  }
};

globalThis.mfsp = {
  init(data) {
    initializedModules.mfsp = data;
  }
};

let loadedMainDictionary = null;
let dictionaryStorageClearCount = 0;

const fakeSpellChecker = {
  setMainDictionary(dictionary, path) {
    loadedMainDictionary = {
      dictionary,
      path
    };

    return true;
  },

  clearStorage() {
    dictionaryStorageClearCount += 1;
  },

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

    if (word === "serveur") {
      return [["serveurs", "serveur"], ["service"]];
    }

    return [["corrigé"]];
  }
};

globalThis.gc_options = {
  lStructOpt: [
    ["basic", [["typo"]]],
    ["verbs", [["infi"]]],
    ["debug", [["idrule"]]]
  ],

  oOptLabel: {
    fr: {
      basic: ["Typographie", ""],
      typo: ["Signes typographiques", ""],
      verbs: ["Verbes", ""],
      infi: ["Infinitif", "Confusion avec une forme verbale."],
      debug: ["Débogage", ""],
      idrule: ["Identifiant des règles", ""]
    }
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
  },

  getOptions() {
    return new Map(fakeOptions);
  },

  getDefaultOptions() {
    return new Map(fakeDefaultOptions);
  },

  setOption(id, enabled) {
    if (fakeOptions.has(id)) {
      fakeOptions.set(id, enabled);
    }
  },

  resetOptions() {
    fakeOptions = new Map(fakeDefaultOptions);
  }
};

const source = fs.readFileSync(
  new URL(
    "../engine-grammalecte/src/main/assets/android_bridge.js",
    import.meta.url
  ),
  "utf8"
);

vm.runInThisContext(source, {
  filename: "android_bridge.js"
});

globalThis.__grammalecteAndroid.init();

assert.deepEqual(loadedFiles, [
  "grammalecte/fr/conj_data.json",
  "grammalecte/fr/phonet_data.json",
  "grammalecte/fr/mfsp_data.json"
]);

assert.equal(
  initializedModules.conj,
  "DATA:grammalecte/fr/conj_data.json"
);

assert.equal(
  initializedModules.phonet,
  "DATA:grammalecte/fr/phonet_data.json"
);

assert.equal(
  initializedModules.mfsp,
  "DATA:grammalecte/fr/mfsp_data.json"
);

assert.deepEqual(loadedContext, {
  context: "JavaScript",
  colorType: "aRGB",
  path: "grammalecte/graphspell/_dictionaries"
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
  JSON.parse(
    globalThis.__grammalecteAndroid.checkWord(
      "correct",
      "fr-FR",
      5
    )
  ),
  {
    valid: true,
    suggestions: []
  }
);

assert.deepEqual(
  JSON.parse(
    globalThis.__grammalecteAndroid.checkWord(
      "fote",
      "fr-FR",
      1
    )
  ),
  {
    valid: false,
    suggestions: ["corrigé"]
  }
);

assert.deepEqual(
  JSON.parse(
    globalThis.__grammalecteAndroid.checkWord(
      "fote",
      "fr-FR",
      0
    )
  ),
  {
    valid: false,
    suggestions: []
  }
);

const ruleOptions = JSON.parse(
  globalThis.__grammalecteAndroid.ruleOptions()
);

assert.deepEqual(ruleOptions, [
  {
    id: "typo",
    groupId: "basic",
    groupLabel: "Typographie",
    label: "Signes typographiques",
    description: "",
    enabled: true,
    defaultEnabled: true
  },
  {
    id: "infi",
    groupId: "verbs",
    groupLabel: "Verbes",
    label: "Infinitif",
    description: "Confusion avec une forme verbale.",
    enabled: true,
    defaultEnabled: true
  }
]);

assert.equal(
  globalThis.__grammalecteAndroid.setRuleOption("infi", false),
  "true"
);

assert.equal(
  JSON.parse(
    globalThis.__grammalecteAndroid.ruleOptions()
  ).find((option) => option.id === "infi").enabled,
  false
);

assert.equal(
  globalThis.__grammalecteAndroid.setRuleOption("unknown", true),
  "false"
);

globalThis.__grammalecteAndroid.resetRuleOptions();

assert.equal(
  JSON.parse(
    globalThis.__grammalecteAndroid.ruleOptions()
  ).find((option) => option.id === "infi").enabled,
  true
);

assert.equal(
  globalThis.__grammalecteAndroid.setDictionary(
    "fr-classic.json"
  ),
  "true"
);

assert.deepEqual(
  loadedMainDictionary,
  {
    dictionary: "fr-classic.json",
    path: "grammalecte/graphspell/_dictionaries"
  }
);

assert.equal(
  dictionaryStorageClearCount,
  1
);

assert.equal(
  globalThis.__grammalecteAndroid.setDictionary(
    "not-a-dictionary.json"
  ),
  "false"
);

assert.equal(
  dictionaryStorageClearCount,
  1
);

console.log("JavaScript bridge contract OK");
