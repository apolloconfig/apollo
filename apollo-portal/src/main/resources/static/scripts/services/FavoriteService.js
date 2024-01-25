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
appService.service('FavoriteService', ['$resource', '$q', 'AppUtil', function ($resource, $q, AppUtil) {
    var resource = $resource('', {}, {
        find_favorites: {
            method: 'GET',
            url: AppUtil.prefixPath() + '/favorites',
            isArray: true
        },
        add_favorite: {
            method: 'POST',
            url: AppUtil.prefixPath() + '/favorites'
        },
        delete_favorite: {
            method: 'DELETE',
            url: AppUtil.prefixPath() + '/favorites/:favoriteId'
        },
        to_top: {
            method: 'PUT',
            url: AppUtil.prefixPath() + '/favorites/:favoriteId'
        }
    });
    return {
        findFavorites: function (userId, appId, page, size) {
            var d = $q.defer();
            resource.find_favorites({
                                        userId: userId,
                                        appId: appId,
                                        page: page,
                                        size: size
                                    }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        addFavorite: function (favorite) {
            var d = $q.defer();
            resource.add_favorite({}, favorite, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        deleteFavorite: function (favoriteId) {
            var d = $q.defer();
            resource.delete_favorite({
                                          favoriteId: favoriteId
                                      }, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        },
        toTop: function (favoriteId) {
            var d = $q.defer();
            resource.to_top({
                                favoriteId: favoriteId
                            }, {}, function (result) {
                d.resolve(result);
            }, function (result) {
                d.reject(result);
            });
            return d.promise;
        }
    }
}]);
