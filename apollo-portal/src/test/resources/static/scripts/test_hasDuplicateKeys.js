/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * Test for hasDuplicateKeys function.
 * Run with Node.js: node test_hasDuplicateKeys.js
 */

// Test harness for hasDuplicateKeys function (not a direct copy)
function hasDuplicateKeys(text) {
    try {
        // Character-level scan because JSON.parse reviver cannot detect
        // duplicates (browser already deduplicates).
        // Note: keys are compared after JSON decoding, so unicode escape equivalences are resolved.
        // Strategy: scan for "key": patterns respecting nesting depth.
        var i = 0;
        var len = text.length;
        var depth = 0;
        var keySets = [];

        while (i < len) {
            var ch = text.charAt(i);
            if (ch === '"') {
                // Read the full string
                var strStart = i;
                i++; // skip opening quote
                while (i < len) {
                    if (text.charAt(i) === '\\') {
                        i += 2; // skip escaped char
                    } else if (text.charAt(i) === '"') {
                        break;
                    } else {
                        i++;
                    }
                }
                var strEnd = i;
                i++; // skip closing quote

                // Check if this string is a key (followed by ':')
                var j = i;
                while (j < len && (text.charAt(j) === ' ' || text.charAt(j) === '\t' ||
                       text.charAt(j) === '\n' || text.charAt(j) === '\r')) {
                    j++;
                }
                if (j < len && text.charAt(j) === ':') {
                    var rawKey = text.substring(strStart + 1, strEnd);
                    var key;
                    try {
                        key = JSON.parse('"' + rawKey + '"');
                    } catch (e) {
                        // If decoding fails, fall back to raw key (invalid JSON, but we still compare raw)
                        key = rawKey;
                    }
                    if (depth >= 0 && depth < keySets.length) {
                        if (key in keySets[depth]) {
                            return true;
                        }
                        keySets[depth][key] = true;
                    }
                }
            } else if (ch === '{') {
                depth++;
                while (keySets.length <= depth) {
                    keySets.push(Object.create(null));
                }
                keySets[depth] = Object.create(null);
                i++;
            } else if (ch === '}') {
                depth--;
                i++;
            } else {
                i++;
            }
        }
        return false;
    } catch (e) {
        return false;
    }
}

// Test cases
function runTests() {
    var passed = 0;
    var total = 0;

    function assert(desc, expected, actual) {
        total++;
        if (expected === actual) {
            passed++;
            console.log('✅ ' + desc);
        } else {
            console.log('❌ ' + desc + ' — expected ' + expected + ', got ' + actual);
        }
    }

    // No duplicates
    assert('Empty object', false, hasDuplicateKeys('{}'));
    assert('Simple object', false, hasDuplicateKeys('{"a":1}'));
    assert('Nested object', false, hasDuplicateKeys('{"a":{"b":2}}'));
    assert('Multiple keys', false, hasDuplicateKeys('{"a":1,"b":2}'));

    // Raw duplicates
    assert('Raw duplicate keys', true, hasDuplicateKeys('{"a":1,"a":2}'));
    assert('Raw duplicate nested', true, hasDuplicateKeys('{"a":1,"b":{"c":3,"c":4}}'));

    // Unicode escape equivalence
    assert('Unicode escape duplicate', true, hasDuplicateKeys('{"\\u0061":1,"a":2}'));
    assert('Unicode escape duplicate reverse', true, hasDuplicateKeys('{"a":1,"\\u0061":2}'));
    assert('Mixed escapes', true, hasDuplicateKeys('{"\\u0061":1,"\\u0062":2,"a":3}')); // a duplicate with \u0061
    assert('Different escapes not duplicate', false, hasDuplicateKeys('{"\\u0061":1,"\\u0062":2}'));

    // Other escape sequences
    assert('Escape n', false, hasDuplicateKeys('{"\\n":1,"a":2}')); // \n is not a letter
    assert('Escape quote', false, hasDuplicateKeys('{"\\"":1,"a":2}'));

    // Invalid JSON (should not crash)
    assert('Invalid JSON missing quote', false, hasDuplicateKeys('{"a:1}'));

    // Edge cases suggested by CodeRabbit
    assert('Object inside array with duplicate', true, hasDuplicateKeys('[{"a":1,"a":2}]'));
    assert('String value resembling a key', false, hasDuplicateKeys('{"a":"b:c","d":1}'));
    assert('Sibling objects with same keys', false, hasDuplicateKeys('{"x":{"a":1},"y":{"a":1}}'));

    console.log('\n' + passed + '/' + total + ' tests passed');
    return passed === total;
}

if (typeof module !== 'undefined' && module.exports) {
    // Node.js environment
    module.exports = { hasDuplicateKeys, runTests };
    if (require.main === module) {
        process.exit(runTests() ? 0 : 1);
    }
} else {
    // Browser environment
    runTests();
}