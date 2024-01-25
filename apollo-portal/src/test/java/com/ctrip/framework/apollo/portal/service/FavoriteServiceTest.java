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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import com.ctrip.framework.apollo.portal.entity.po.Favorite;
import com.ctrip.framework.apollo.portal.repository.FavoriteRepository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

public class FavoriteServiceTest extends AbstractIntegrationTest {

  @Autowired
  private FavoriteService favoriteService;
  @Autowired
  private FavoriteRepository favoriteRepository;

  private String testUser = "apollo";

  @Before
  public void before() {

  }

  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testAddNormalFavorite() {
    String testApp = "testApp";
    Favorite favorite = instanceOfFavorite(testUser, testApp);
    favoriteService.addFavorite(favorite);

    List<Favorite> createdFavorites = favoriteService.search(testUser, testApp, PageRequest.of(0, 10));

    Assert.assertEquals(1, createdFavorites.size());

    Assert.assertEquals(FavoriteService.POSITION_DEFAULT, createdFavorites.get(0).getPosition());
    Assert.assertEquals(testUser, createdFavorites.get(0).getUserId());
    Assert.assertEquals(testApp, createdFavorites.get(0).getAppId());

  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testAddFavoriteErrorUser() {
    String testApp = "testApp";
    Favorite favorite = instanceOfFavorite("errorUser", testApp);
    favoriteService.addFavorite(favorite);
  }

  @Test
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testSearchByUserId() {
    List<Favorite> favorites = favoriteService.search(testUser, null, PageRequest.of(0, 10));
    Assert.assertEquals(4, favorites.size());
  }

  @Test
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testSearchByAppId() {
    List<Favorite> favorites = favoriteService.search(null, "test0621-04", PageRequest.of(0, 10));
    Assert.assertEquals(3, favorites.size());
  }

  @Test
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testSearchByAppIdAndUserId() {
    List<Favorite> favorites = favoriteService.search(testUser, "test0621-04", PageRequest.of(0, 10));
    Assert.assertEquals(1, favorites.size());
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testSearchWithErrorParams() {
    favoriteService.search(null, null, PageRequest.of(0, 10));
  }

  @Test
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteFavorite() {
    long legalFavoriteId = 21L;
    favoriteService.deleteFavorite(legalFavoriteId);
    Assert.assertNull(favoriteRepository.findById(legalFavoriteId).orElse(null));
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testDeleteFavoriteFail() {
    long anotherPersonFavoriteId = 23L;
    favoriteService.deleteFavorite(anotherPersonFavoriteId);
    Assert.assertNull(favoriteRepository.findById(anotherPersonFavoriteId).orElse(null));
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testAdjustFavoriteError() {
    long anotherPersonFavoriteId = 23;
    favoriteService.adjustFavoriteToFirst(anotherPersonFavoriteId);
  }

  @Test
  @Sql(scripts = "/sql/favorites/favorites.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testAdjustFavorite() {
    long toAdjustFavoriteId = 20;
    favoriteService.adjustFavoriteToFirst(toAdjustFavoriteId);

    List<Favorite> favorites = favoriteService.search(testUser, null, PageRequest.of(0, 10));
    Favorite firstFavorite = favorites.get(0);
    Favorite secondFavorite = favorites.get(1);

    Assert.assertEquals(toAdjustFavoriteId, firstFavorite.getId());
    Assert.assertEquals(firstFavorite.getPosition() + 1, secondFavorite.getPosition());
  }


  private Favorite instanceOfFavorite(String userId, String appId) {
    Favorite favorite = new Favorite();
    favorite.setAppId(appId);
    favorite.setUserId(userId);
    favorite.setDataChangeCreatedBy(userId);
    favorite.setDataChangeLastModifiedBy(userId);
    return favorite;
  }

}
