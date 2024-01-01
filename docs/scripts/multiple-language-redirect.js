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
 * A custom docsify plugin. Let user switch to another language more convenient.
 * Reference https://docsify.js.org/#/write-a-plugin
 * 
 * @author wxq
 */

/* JavaScript's function below */

function findCurrentLanguagePrefix(languagePrefixs, path) {
    languagesPrefixOrderByLengthDesc = languagePrefixs.sort(function (a, b) {
        return b.length - a.length;
    })
    for (const languagePrefix of languagesPrefixOrderByLengthDesc) {
        if (path.startsWith(languagePrefix)) {
            return languagePrefix;
        }
    }
    console.error("cannot find language prefix in path", path, "through", languagePrefixs)
    return '/';
}

function generateLanguagePrefix2Path(languagePrefixs, path) {
    const currentLanguagePrefix = findCurrentLanguagePrefix(languagePrefixs, path);
    languagePrefix2Path = {};
    for (const targetLanguagePrefix of languagesPrefixOrderByLengthDesc) {
        languagePrefix2Path[targetLanguagePrefix] = path.replace(currentLanguagePrefix, targetLanguagePrefix);
    }
    return languagePrefix2Path;
}

/**
 * find list item in navbar by its name.
 */
function findTranslationsListItem(translationsListItemName) {
    const nav = document.querySelector("nav");
    if (null == nav) {
        return null;
    }

    const ul = nav.querySelector("ul");
    if (null == ul) {
        return null;
    }

    const listItems = ul.querySelectorAll("li");
    if (null == listItems) {
        return null;
    }

    for (const listItem of listItems) {
        if (listItem.innerText.includes(translationsListItemName)) {
            return listItem;
        }
    }

    return null;
}

function walkElementInTranslationsListItem(translationsListItem, elementName, visitor) {
    const elements = translationsListItem.querySelectorAll(elementName);
    for (const element of elements) {
        visitor(element);
    }
}

function removeSharpPrefixInHref(href) {
    if (href.startsWith("#")) {
        // delete '#' in prefix, example: '#/zh-cn/' => '/zh-cn/'
        return href.substring(1);
    } else {
        return href;
    }
}

function addSharpToPrefix(path) {
    if (path.startsWith("#")) {
        return path;
    } else {
        return "#" + path;
    }
}

/**
 * find language which user config in '_navbar.md'.
 * 
 * @returns a list of language prefix config in '_navbar.md'
 */
function resolveLanguagePrefixsFromListItem(translationsListItem) {
    languagePrefixs = []
    walkElementInTranslationsListItem(translationsListItem, 'a', function (aElement) {
        const href = aElement.getAttribute("href");
        languagePrefix = removeSharpPrefixInHref(href);
        languagePrefixs.push(languagePrefix);
    });

    // a return example: ['/', '/zh-cn/', '/de-de/', '/es/', '/ru-ru/']
    return languagePrefixs;
}

function changeLinkInTranslationsListItem(currrentPath, translationsListItem) {
    const languagePrefixs = resolveLanguagePrefixsFromListItem(translationsListItem);
    const languagePrefix2Path = generateLanguagePrefix2Path(languagePrefixs, currrentPath);
    walkElementInTranslationsListItem(translationsListItem, 'a', function (aElement) {
        const href = aElement.getAttribute("href");
        const languagePrefix = removeSharpPrefixInHref(href);
        const newPath = languagePrefix2Path[languagePrefix];
        const newHref = addSharpToPrefix(newPath);
        aElement.setAttribute("href", newHref);
        // console.log(href, "=>", newHref);
    });
}

/**
 * When user click another language in navbar's Translations, 
 * website's path will change to path which corresponding to current language.
 * @param {string} name item name 'Translations' in navbar
 */
function generateMultipleLanguagesNavbarPluginByListItemName(name) {
    return function (hook, vm) {
        const bindEventForChangeHrefInNavbar = () => {
            // when user's mouse down, chanage href in navbar
            document.addEventListener("mousedown", _mouseEvent => {
                const currrentPath = vm.route.path;
                // find navbar list item by hard code name
                const translationsListItemName = name;
                const translationsListItem = findTranslationsListItem(translationsListItemName);
                if (null == translationsListItem) {
                    console.warn("there is no navbar or ", translationsListItemName, "in current path", currrentPath);
                } else {
                    changeLinkInTranslationsListItem(currrentPath, translationsListItem);
                }
            });
        };
    
        hook.init(bindEventForChangeHrefInNavbar);
    };
}
