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
appUtil.service('AppUtil', ['toastr', '$window', '$q', '$translate', 'prefixLocation', function (toastr, $window, $q, $translate, prefixLocation) {

    function parseErrorMsg(response) {
        if (response.status == -1) {
            return $translate.instant('Common.LoginExpiredTips');
        }
        var msg = "Code:" + response.status;
        if (response.data.message != null) {
            msg += " Msg:" + response.data.message;
        }
        return msg;
    }

    function parsePureErrorMsg(response) {
        if (response.status == -1) {
            return $translate.instant('Common.LoginExpiredTips');
        }
        if (response.data.message != null) {
            return response.data.message;
        }
        return "";
    }

    function ajax(resource, requestParams, requestBody) {
        var d = $q.defer();
        if (requestBody) {
            resource(requestParams, requestBody, function (result) {
                d.resolve(result);
            },
                function (result) {
                    d.reject(result);
                });
        } else {
            resource(requestParams, function (result) {
                d.resolve(result);
            },
                function (result) {
                    d.reject(result);
                });
        }

        return d.promise;
    }

    /**
     * Check if a JSON string contains duplicate keys at any nesting level.
     * Performs a character-level scan to detect duplicate keys per object scope,
     * since JSON.parse silently deduplicates keys.
     * Returns true if duplicate keys are found.
     */
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

    return {
        prefixPath: function(){
            return prefixLocation;
        },
        errorMsg: parseErrorMsg,
        pureErrorMsg: parsePureErrorMsg,
        ajax: ajax,
        showErrorMsg: function (response, title) {
            toastr.error(parseErrorMsg(response), title);
        },
        parseParams: function (query, notJumpToHomePage) {
            if (!query) {
                //如果不传这个参数或者false则返回到首页(参数出错)
                if (!notJumpToHomePage) {
                    $window.location.href = prefixLocation + '/index.html';
                } else {
                    return {};
                }
            }
            if (query.indexOf('/') == 0) {
                query = query.substring(1, query.length);
            }

            var anchorIndex = query.indexOf('#');
            if (anchorIndex >= 0) {
                query = query.substring(0, anchorIndex);
            }

            var params = query.split("&");
            var result = {};
            params.forEach(function (param) {
                var kv = param.split("=");
                result[kv[0]] = decodeURIComponent(kv[1]);
            });
            return result;
        },
        /**
         * Filters and returns entities with successful status (code === 200).
         * Returns the full entity object (new OpenAPI contract).
         * For legacy response format with entity.body, use collectData instead.
         */
        gatherData: function (response) {
            var data = [];
            response.forEach(function (entity) {
                if (entity.code === 200) {
                  data.push(entity);
                } else {
                  toastr.warning(entity.message);
                }
            });
            return data;
        },
        collectData: function (response) {
            var data = [];
            response.entities.forEach(function (entity) {
                if (entity.code == 200) {
                    data.push(entity.body);
                } else {
                    toastr.warning(entity.message);
                }
            });
            return data;
        },
        showModal: function (modal) {
            $(modal).modal("show");
        },
        hideModal: function (modal) {
            $(modal).modal("hide");
        },
        checkIPV4: function (ip) {
            return /^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$|^(([a-zA-Z]|[a-zA-Z][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z]|[A-Za-z][A-Za-z0-9\-]*[A-Za-z0-9])$|^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/.test(ip);
        },
        hasDuplicateKeys: hasDuplicateKeys
    }
}]);
