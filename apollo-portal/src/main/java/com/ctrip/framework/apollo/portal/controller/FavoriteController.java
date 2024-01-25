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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.portal.entity.po.Favorite;
import com.ctrip.framework.apollo.portal.service.FavoriteService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FavoriteController {

  private final FavoriteService favoriteService;

  public FavoriteController(final FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }


  @PostMapping("/favorites")
  public Favorite addFavorite(@RequestBody Favorite favorite) {
    return favoriteService.addFavorite(favorite);
  }


  @GetMapping("/favorites")
  public List<Favorite> findFavorites(@RequestParam(value = "userId", required = false) String userId,
                                      @RequestParam(value = "appId", required = false) String appId,
                                      Pageable page) {
    return favoriteService.search(userId, appId, page);
  }


  @DeleteMapping("/favorites/{favoriteId}")
  public void deleteFavorite(@PathVariable long favoriteId) {
    favoriteService.deleteFavorite(favoriteId);
  }


  @PutMapping("/favorites/{favoriteId}")
  public void toTop(@PathVariable long favoriteId) {
    favoriteService.adjustFavoriteToFirst(favoriteId);
  }

}
