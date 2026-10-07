"use strict";

(function () {
    function unique(values, limit) {
        const result = [];
        const seen = new Set();
        for (const value of values || []) {
            if (typeof value !== "string" || value.length === 0 || seen.has(value)) {
                continue;
            }
            seen.add(value);
            result.push(value);
            if (result.length >= limit) {
                break;
            }
        }
        return result;
    }

    function flattenSuggestions(groups, limit) {
        const result = [];
        for (const group of groups || []) {
            for (const suggestion of group || []) {
                result.push(suggestion);
            }
        }
        return unique(result, limit);
    }

    globalThis.__grammalecteAndroid = {
        init: function () {
            conj.init(
                helpers.loadFile("grammalecte/fr/conj_data.json")
            );

            phonet.init(
                helpers.loadFile("grammalecte/fr/phonet_data.json")
            );

            mfsp.init(
                helpers.loadFile("grammalecte/fr/mfsp_data.json")
            );

            gc_engine.load(
                "JavaScript",
                "aRGB",
                "grammalecte/graphspell/_dictionaries"
            );
        },

        check: function (text, localeTag) {
            const country = String(localeTag || "fr-FR").split("-")[1] || "FR";
            const issues = [];

            for (const error of Array.from(gc_engine.parse(text, country))) {
                issues.push({
                    start: error.nStart,
                    end: error.nEnd,
                    suggestions: unique(error.aSuggestions || [], 8),
                    message: error.sMessage || "",
                    ruleId: error.sRuleId || "",
                    kind: "GRAMMAR"
                });
            }

            const spellChecker = gc_engine.getSpellChecker();
            for (const token of spellChecker.parseParagraph(text)) {
                issues.push({
                    start: token.nStart,
                    end: token.nEnd,
                    suggestions: flattenSuggestions(spellChecker.suggest(token.sValue, 8), 8),
                    message: "",
                    ruleId: "SPELLING",
                    kind: "SPELLING"
                });
            }

            return JSON.stringify(issues);
        },

        ruleOptions: function () {
            const labels = gc_options.oOptLabel.fr || {};
            const current = gc_engine.getOptions();
            const defaults = gc_engine.getDefaultOptions();
            const result = [];

            for (const [groupId, rows] of gc_options.lStructOpt) {
                if (groupId === "debug") {
                    continue;
                }

                const groupMetadata = labels[groupId] || [groupId, ""];

                for (const row of rows) {
                    for (const optionId of row) {
                        if (!current.has(optionId)) {
                            continue;
                        }

                        const metadata = labels[optionId] || [optionId, ""];

                        result.push({
                            id: optionId,
                            groupId: groupId,
                            groupLabel: String(groupMetadata[0] || groupId),
                            label: String(metadata[0] || optionId),
                            description: String(metadata[1] || ""),
                            enabled: Boolean(current.get(optionId)),
                            defaultEnabled: Boolean(defaults.get(optionId))
                        });
                    }
                }
            }

            return JSON.stringify(result);
        },

        setRuleOption: function (id, enabled) {
            const optionId = String(id || "");
            const current = gc_engine.getOptions();

            if (!current.has(optionId)) {
                return "false";
            }

            gc_engine.setOption(optionId, Boolean(enabled));
            return "true";
        },

        resetRuleOptions: function () {
            gc_engine.resetOptions();
            return "ok";
        },

        setDictionary: function (dictionaryFile) {
            const fileName = String(dictionaryFile || "");

            const allowed = new Set([
                "fr-allvars.json",
                "fr-classic.json",
                "fr-reform.json"
            ]);

            if (!allowed.has(fileName)) {
                return "false";
            }

            const spellChecker = gc_engine.getSpellChecker();

            const loaded = spellChecker.setMainDictionary(
                fileName,
                "grammalecte/graphspell/_dictionaries"
            );

            if (loaded) {
                spellChecker.clearStorage();
            }

            return loaded ? "true" : "false";
        },

        setPersonalWords: function (wordsJson) {
            let rawWords;

            try {
                rawWords = JSON.parse(String(wordsJson || "[]"));
            } catch (error) {
                return "false";
            }

            if (!Array.isArray(rawWords) || rawWords.length > 2048) {
                return "false";
            }

            const words = [];
            const seen = new Set();

            for (const value of rawWords) {
                if (typeof value !== "string") {
                    return "false";
                }

                const word = value.trim();

                if (
                    word.length === 0 ||
                    word.length > 64 ||
                    /\s/.test(word)
                ) {
                    return "false";
                }

                if (!seen.has(word)) {
                    seen.add(word);
                    words.push(word);
                }
            }

            const spellChecker = gc_engine.getSpellChecker();

            if (words.length === 0) {
                spellChecker.setPersonalDictionary(null);
                spellChecker.clearStorage();
                return "true";
            }

            try {
                const entries = words.map(function (word) {
                    return [word, word, ":X"];
                });

                const dictionary = new DAWG(
                    entries,
                    "S",
                    "fr",
                    "Français",
                    "Personnel",
                    "Dictionnaire personnel"
                ).createBinaryJSON();

                const loaded =
                    spellChecker.setPersonalDictionary(dictionary);

                if (loaded) {
                    spellChecker.clearStorage();
                }

                return loaded ? "true" : "false";
            } catch (error) {
                return "false";
            }
        },

        checkWord: function (word, localeTag, limit) {
            void localeTag;
            const spellChecker = gc_engine.getSpellChecker();
            const max = Math.max(0, Number(limit || 0));
            if (spellChecker.isValid(word)) {
                return JSON.stringify({ valid: true, suggestions: [] });
            }
            return JSON.stringify({
                valid: false,
                suggestions: max === 0 ? [] : flattenSuggestions(spellChecker.suggest(word, max), max)
            });
        }
    };
})();
