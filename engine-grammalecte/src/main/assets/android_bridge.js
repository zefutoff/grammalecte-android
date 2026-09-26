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
            gc_engine.load("JavaScript", "aRGB", "grammalecte/graphspell/_dictionaries");
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
