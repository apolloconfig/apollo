/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.core.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DNSUtil {

  public static List<String> resolve(String domainName) throws UnknownHostException {
    List<String> result = new ArrayList<>();

    InetAddress[] addresses = InetAddress.getAllByName(domainName);
    if (addresses != null) {
      for (InetAddress addr : addresses) {
        result.add(addr.getHostAddress());
      }
    }

    return result;
  }

}
