#!/usr/bin/env python3
#
# Copyright 2026 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the
# License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
# either express or implied. See the License for the specific language governing permissions
# and limitations under the License.

import tempfile
from pathlib import Path
import unittest

from collect_portal_frontend_urls import collect_service_urls, main, render_markdown


class CollectPortalFrontendUrlsTest(unittest.TestCase):

  def test_collects_url_properties_and_resource_base_paths(self):
    with tempfile.TemporaryDirectory() as tmpdir:
      service_file = Path(tmpdir) / "SampleService.js"
      service_file.write_text(
          """
appService.service('SampleService', ['$resource', 'AppUtil', function ($resource, AppUtil) {
  var resource = $resource(AppUtil.prefixPath() + '/apps/:appId', {}, {
    find_apps: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/openapi/v1/apps'
    },
    find_config: {
      method: 'GET',
      url: AppUtil.prefixPath()
        + '/server/portal-db/config/find-all-config'
    },
    get_lock: {
      method: 'GET',
      url: 'apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/lock-info'
    }
  });
}]);
""",
          encoding="utf-8",
      )

      urls = collect_service_urls(service_file)

    self.assertEqual(4, len(urls))
    self.assertEqual("/apps/:appId", urls[0].path)
    self.assertEqual("RESOURCE_BASE", urls[0].method)
    self.assertEqual("/openapi/v1/apps", urls[1].path)
    self.assertEqual("OpenAPI", urls[1].surface)
    self.assertEqual("/server/portal-db/config/find-all-config", urls[2].path)
    self.assertTrue(urls[2].prefix_path)
    self.assertEqual(
        "/apps/:appId/envs/:env/clusters/:clusterName/namespaces/:namespaceName/lock-info",
        urls[3].path,
    )
    self.assertFalse(urls[3].prefix_path)

  def test_renders_summary(self):
    with tempfile.TemporaryDirectory() as tmpdir:
      service_file = Path(tmpdir) / "SampleService.js"
      service_file.write_text(
          """
appService.service('SampleService', ['$resource', 'AppUtil', function ($resource, AppUtil) {
  var resource = $resource('', {}, {
    find_apps: {
      method: 'GET',
      url: AppUtil.prefixPath() + '/openapi/v1/apps'
    }
  });
}]);
""",
          encoding="utf-8",
      )
      markdown = render_markdown(collect_service_urls(service_file), "en")

    self.assertIn("URL entries: 1", markdown)
    self.assertIn("OpenAPI entries: 1", markdown)
    self.assertIn("`/openapi/v1/apps`", markdown)

  def test_main_fails_for_invalid_services_dir(self):
    with tempfile.TemporaryDirectory() as tmpdir:
      missing_dir = Path(tmpdir) / "missing"

      exit_code = main(["--services-dir", str(missing_dir)])

    self.assertEqual(1, exit_code)


if __name__ == "__main__":
  unittest.main()
