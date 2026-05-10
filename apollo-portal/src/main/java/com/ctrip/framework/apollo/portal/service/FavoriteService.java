/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Favorite;
import com.ctrip.framework.apollo.portal.repository.FavoriteRepository;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.google.common.base.Strings;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class FavoriteService {

  public static final long POSITION_DEFAULT = 10000;

  private final FavoriteRepository favoriteRepository;
  private final UserService userService;

  public FavoriteService(final FavoriteRepository favoriteRepository,
      final UserService userService) {
    this.favoriteRepository = favoriteRepository;
    this.userService = userService;
  }


  public Favorite addFavorite(Favorite favorite, String loginUserId) {
    UserInfo user = userService.findByUserId(favorite.getUserId());
    if (user == null) {
      throw BadRequestException.userNotExists(favorite.getUserId());
    }

    // user can only add himself favorite app
    if (!Objects.equals(loginUserId, user.getUserId())) {
      throw new BadRequestException(
          "add favorite fail. " + "because favorite's user is not current login user.");
    }

    Favorite checkedFavorite =
        favoriteRepository.findByUserIdAndAppId(loginUserId, favorite.getAppId());
    if (checkedFavorite != null) {
      return checkedFavorite;
    }

    favorite.setPosition(POSITION_DEFAULT);
    favorite.setDataChangeCreatedBy(user.getUserId());
    favorite.setDataChangeLastModifiedBy(user.getUserId());

    return favoriteRepository.save(favorite);
  }


  public List<Favorite> search(String userId, String appId, Pageable page, String loginUserId) {
    boolean isUserIdEmpty = Strings.isNullOrEmpty(userId);
    boolean isAppIdEmpty = Strings.isNullOrEmpty(appId);

    if (isAppIdEmpty && isUserIdEmpty) {
      throw new BadRequestException("user id and app id can't be empty at the same time");
    }

    if (!isUserIdEmpty) {
      // user can only search his own favorite app
      if (!Objects.equals(loginUserId, userId)) {
        userId = loginUserId;
      }
    }

    // search by userId
    if (isAppIdEmpty) {
      return favoriteRepository.findByUserIdOrderByPositionAscDataChangeCreatedTimeAsc(userId,
          page);
    }

    // search by appId
    if (isUserIdEmpty) {
      return favoriteRepository.findByAppIdOrderByPositionAscDataChangeCreatedTimeAsc(appId, page);
    }

    // search by userId and appId
    return Collections.singletonList(favoriteRepository.findByUserIdAndAppId(userId, appId));
  }

  public void deleteFavorite(long favoriteId, String loginUserId) {
    Favorite favorite = favoriteRepository.findById(favoriteId).orElse(null);

    checkUserOperatePermission(favorite, loginUserId);

    favoriteRepository.delete(favorite);
  }

  public void adjustFavoriteToFirst(long favoriteId, String loginUserId) {
    Favorite favorite = favoriteRepository.findById(favoriteId).orElse(null);

    checkUserOperatePermission(favorite, loginUserId);

    String userId = favorite.getUserId();
    Favorite firstFavorite =
        favoriteRepository.findFirstByUserIdOrderByPositionAscDataChangeCreatedTimeAsc(userId);
    long minPosition = firstFavorite.getPosition();

    favorite.setPosition(minPosition - 1);

    favoriteRepository.save(favorite);
  }

  private void checkUserOperatePermission(Favorite favorite, String loginUserId) {
    if (favorite == null) {
      throw new BadRequestException("favorite not exist");
    }

    if (!Objects.equals(loginUserId, favorite.getUserId())) {
      throw new BadRequestException("can not operate other person's favorite");
    }
  }

  public void batchDeleteByAppId(String appId, String operator) {
    favoriteRepository.batchDeleteByAppId(appId, operator);
  }
}
