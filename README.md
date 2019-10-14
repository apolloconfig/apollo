<img src="https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/logo/logo-simple.png" alt="apollo-logo" width="60%">

# Apollo - A reliable configuration management system

[![Build Status](https://travis-ci.org/ctripcorp/apollo.svg?branch=master)](https://travis-ci.org/ctripcorp/apollo)
[![GitHub release](https://img.shields.io/github/release/ctripcorp/apollo.svg)](https://github.com/ctripcorp/apollo/releases)
[![Maven Central Repo](https://img.shields.io/maven-central/v/com.ctrip.framework.apollo/apollo.svg)](https://mvnrepository.com/artifact/com.ctrip.framework.apollo/apollo-client)
[![Coverage Status](https://coveralls.io/repos/github/ctripcorp/apollo/badge.svg?branch=master)](https://coveralls.io/github/ctripcorp/apollo?branch=master)
<a href="https://scan.coverity.com/projects/ctripcorp-apollo">
  <img alt="Coverity Scan Build Status" src="https://img.shields.io/coverity/scan/8244.svg"/>
</a>
[![codecov.io](https://codecov.io/github/ctripcorp/apollo/coverage.svg?branch=master)](https://codecov.io/github/ctripcorp/apollo?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


Apollo（阿波罗）是携程框架部门研发的分布式配置中心，能够集中化管理应用不同环境、不同集群的配置，配置修改后能够实时推送到应用端，并且具备规范的权限、流程治理等特性，适用于微服务配置管理场景。

服务端基于Spring Boot和Spring Cloud开发，打包后可以直接运行，不需要额外安装Tomcat等应用容器。

Java客户端不依赖任何框架，能够运行于所有Java运行时环境，同时对Spring/Spring Boot环境也有较好的支持。

.Net客户端不依赖任何框架，能够运行于所有.Net运行时环境。

更多产品介绍参见[Apollo配置中心介绍](https://github.com/ctripcorp/apollo/wiki/Apollo%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83%E4%BB%8B%E7%BB%8D)

本地快速部署请参见[Quick Start](https://github.com/ctripcorp/apollo/wiki/Quick-Start)

演示环境（Demo）:
- [106.12.25.204:8070](http://106.12.25.204:8070/)
- 账号/密码:apollo/admin

> 如访问github速度缓慢，可以访问[gitee镜像](https://gitee.com/nobodyiam/apollo)，不定期同步

# Screenshots
![配置界面](https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/apollo-home-screenshot.png)

# Features
* **统一管理不同环境、不同集群的配置**
  * Apollo提供了一个统一界面集中式管理不同环境（environment）、不同集群（cluster）、不同命名空间（namespace）的配置。
  * 同一份代码部署在不同的集群，可以有不同的配置，比如zk的地址等
  * 通过命名空间（namespace）可以很方便的支持多个不同应用共享同一份配置，同时还允许应用对共享的配置进行覆盖

* **配置修改实时生效（热发布）**
  * 用户在Apollo修改完配置并发布后，客户端能实时（1秒）接收到最新的配置，并通知到应用程序。

* **版本发布管理**
  * 所有的配置发布都有版本概念，从而可以方便的支持配置的回滚。

* **灰度发布**
  * 支持配置的灰度发布，比如点了发布后，只对部分应用实例生效，等观察一段时间没问题后再推给所有应用实例。

* **权限管理、发布审核、操作审计**
  * 应用和配置的管理都有完善的权限管理机制，对配置的管理还分为了编辑和发布两个环节，从而减少人为的错误。
  * 所有的操作都有审计日志，可以方便的追踪问题。

* **客户端配置信息监控**
  * 可以方便的看到配置在被哪些实例使用

* **提供Java和.Net原生客户端**
  * 提供了Java和.Net的原生客户端，方便应用集成
  * 支持Spring Placeholder, Annotation和Spring Boot的ConfigurationProperties，方便应用使用（需要Spring 3.1.1+）
  * 同时提供了Http接口，非Java和.Net应用也可以方便的使用

* **提供开放平台API**
  * Apollo自身提供了比较完善的统一配置管理界面，支持多环境、多数据中心配置管理、权限、流程治理等特性。
  * 不过Apollo出于通用性考虑，对配置的修改不会做过多限制，只要符合基本的格式就能够保存。
  * 在我们的调研中发现，对于有些使用方，它们的配置可能会有比较复杂的格式，如xml, json，需要对格式做校验。
  * 还有一些使用方如DAL，不仅有特定的格式，而且对输入的值也需要进行校验后方可保存，如检查数据库、用户名和密码是否匹配。
  * 对于这类应用，Apollo支持应用方通过开放接口在Apollo进行配置的修改和发布，并且具备完善的授权和权限控制

* **部署简单**
  * 配置中心作为基础服务，可用性要求非常高，这就要求Apollo对外部依赖尽可能地少
  * 目前唯一的外部依赖是MySQL，所以部署非常简单，只要安装好Java和MySQL就可以让Apollo跑起来
  * Apollo还提供了打包脚本，一键就可以生成所有需要的安装包，并且支持自定义运行时参数

# Usage
  1. [Apollo使用指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  2. [Java客户端使用指南](https://github.com/ctripcorp/apollo/wiki/Java%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  3. [.Net客户端使用指南](https://github.com/ctripcorp/apollo/wiki/.Net%E5%AE%A2%E6%88%B7%E7%AB%AF%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
  4. [其它语言客户端接入指南](https://github.com/ctripcorp/apollo/wiki/%E5%85%B6%E5%AE%83%E8%AF%AD%E8%A8%80%E5%AE%A2%E6%88%B7%E7%AB%AF%E6%8E%A5%E5%85%A5%E6%8C%87%E5%8D%97)
  5. [Apollo开放平台接入指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0)
  6. [Apollo使用场景和示例代码](https://github.com/ctripcorp/apollo-use-cases)

# Design
  * [Apollo配置中心设计](https://github.com/ctripcorp/apollo/wiki/Apollo%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83%E8%AE%BE%E8%AE%A1)
  * [Apollo核心概念之“Namespace”](https://github.com/ctripcorp/apollo/wiki/Apollo%E6%A0%B8%E5%BF%83%E6%A6%82%E5%BF%B5%E4%B9%8B%E2%80%9CNamespace%E2%80%9D)
  * [Apollo配置中心架构剖析](https://mp.weixin.qq.com/s/-hUaQPzfsl9Lm3IqQW3VDQ)
  * [Apollo源码解析](http://www.iocoder.cn/categories/Apollo/)（据说Apollo非常适合作为初学者第一个通读源码学习的分布式中间件产品）

# Development
  * [Apollo开发指南](https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97)
  * Code Styles
    * [Eclipse Code Style](https://github.com/ctripcorp/apollo/blob/master/apollo-buildtools/style/eclipse-java-google-style.xml)
    * [Intellij Code Style](https://github.com/ctripcorp/apollo/blob/master/apollo-buildtools/style/intellij-java-google-style.xml)

# Deployment
  * [Quick Start](https://github.com/ctripcorp/apollo/wiki/Quick-Start)
  * [分布式部署指南](https://github.com/ctripcorp/apollo/wiki/%E5%88%86%E5%B8%83%E5%BC%8F%E9%83%A8%E7%BD%B2%E6%8C%87%E5%8D%97)

# Release Notes
  * [版本发布历史](https://github.com/ctripcorp/apollo/releases)

# FAQ
  * [常见问题回答](https://github.com/ctripcorp/apollo/wiki/FAQ)
  * [部署&开发遇到的常见问题](https://github.com/ctripcorp/apollo/wiki/%E9%83%A8%E7%BD%B2&%E5%BC%80%E5%8F%91%E9%81%87%E5%88%B0%E7%9A%84%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98)

# Presentation
  * [携程开源配置中心Apollo的设计与实现](http://www.itdks.com/dakalive/detail/3420)
    * [Slides](https://myslide.cn/slides/10168)
  * [配置中心，让微服务更『智能』](https://2018.qconshanghai.com/presentation/799)
    * [Slides](https://myslide.cn/slides/10035)

# Publication
  * [开源配置中心Apollo的设计与实现](https://www.infoq.cn/article/open-source-configuration-center-apollo)
  * [配置中心，让微服务更『智能』](https://mp.weixin.qq.com/s/iDmYJre_ULEIxuliu1EbIQ)

# Support
<table>
  <thead>
    <th>Apollo技术支持④群<br />群号：516773934 </th>
    <th>Apollo技术支持③群<br />群号：742035428（已满）</th>
    <th>Apollo技术支持②群<br />群号：904287263（已满）</th>
    <th>Apollo技术支持①群<br />群号：375526581（已满）</th>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/tech-support-qq-4.png" alt="tech-support-qq-4"></td>
      <td><img src="https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/tech-support-qq-3.png" alt="tech-support-qq-3"></td>
      <td><img src="https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/tech-support-qq-2.png" alt="tech-support-qq-2"></td>
      <td><img src="https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/tech-support-qq-1.png" alt="tech-support-qq-1"></td>
    </tr>
  </tbody>
</table>

# Contribution

Please make sure to read the [Contributing Guide](https://github.com/ctripcorp/apollo/blob/master/CONTRIBUTING.md) before making a pull request.

Thanks for all the people who contributed to Apollo!

<a href="https://github.com/ctripcorp/apollo/graphs/contributors"><img src="https://opencollective.com/apollo/contributors.svg?width=880&button=false" /></a>

# License



Apache License
                           Version 2.0, January 2004
                        https://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2019 Rolando Gopez Lacuata

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


# Known Users

> 按照登记顺序排序，更多接入公司，欢迎在[https://github.com/ctripcorp/apollo/issues/451](https://github.com/ctripcorp/apollo/issues/451)登记（仅供开源用户参考）

![携程](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ctrip.png)
![青石证券](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/bluestone.png)
![沙绿](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/sagreen.png)
![航旅纵横](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/umetrip.jpg)
![58转转](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zhuanzhuan.png)
![蜂助手](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/phone580.png)
![海南航空](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hainan-airlines.png)
![CVTE](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/cvte.png)
![明博教育](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mainbo.jpg)
![麻袋理财](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/madailicai.png)
![美行科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mxnavi.jpg)
![首展科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/fshows.jpg)
![易微行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/feezu.png)
![人才加](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/rencaijia.png)
![凯京集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/keking.png)
![乐刻运动](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/leoao.png)
![大疆](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/dji.png)
![快看漫画](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/kkmh.png)
![我来贷](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/wolaidai.png)
![虚实软件](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xsrj.png)
![网易严选](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yanxuan.png)
![视觉中国](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/sjzg.png)
![资产360](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zc360.png)
![亿咖通](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ecarx.png)
![5173](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/5173.png)
![沪江](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hujiang.png)
![网易云基础服务](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/163yun.png)
![现金巴士](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/cash-bus.png)
![锤子科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/smartisan.png)
![头等仓](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/toodc.png)
![吉祥航空](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/juneyaoair.png)
![263移动通信](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/263mobile.png)
![投投金融](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/toutoujinrong.png)
![每天健康](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mytijian.png)
![麦芽金服](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/maiyabank.png)
![蜂向科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/fengunion.png)
![即科金融](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/geex-logo.png)
![贝壳网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/beike.png)
![有赞](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/youzan.png)
![云集汇通](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yunjihuitong.png)
![犀牛瀚海科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/rhinotech.png)
![农信互联](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/nxin.png)
![蘑菇租房](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mgzf.png)
![狐狸金服](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/huli-logo.png)
![漫道集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mandao.png)
![怪兽充电](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/enmonster.png)
![南瓜租房](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/nanguazufang.png)
![石投金融](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/shitoujinrong.png)
![土巴兔](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/tubatu.png)
![平安银行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/payh_logo.png)
![新新贷](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xinxindai.png)
![中国华戎科技集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/chrtc.png)
![涂鸦智能](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/tuya_logo.png)
![立创商城](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/szlcsc.jpg)
![乐赚金服](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hairongyi.png)
![开心汽车](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/kxqc.png)
![乐赚金服](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ppcredit.png)
![普元信息](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/primeton.png)
![医帮管家](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hoskeeper.png)
![付啦信用卡管家](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/fula.png)
![悠哉网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/uzai.png)
![梧桐诚选](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/91wutong.png)
![拍拍贷](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ppdai.png)
![信用飞](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xinyongfei.png)
![丁香园](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/dxy.png)
![国槐科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ghtech.png)
![亲宝宝](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/qbb.png)
![华为视频直播](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/huawei_logo.png)
![微播易](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/weiboyi.png)
![欧飞](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ofpay.png)
![迷说](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mishuo.png)
![一下科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yixia.png)
![DaoCloud](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/daocloud.png)
![汽摩交易所](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/cnvex.png)
![好未来教育集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/100tal.png)
![猎户星空](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ainirobot.png)
![卓健科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zhuojian.png)
![银江股份](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/enjoyor.png)
![途虎养车](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/tuhu.png)
![河姆渡](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/homedo.png)
![新网银行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xwbank.png)
![中旅安信云贷](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ctspcl.png)
![美柚](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/meiyou.png)
![震坤行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zkh-logo.png)
![万谷盛世](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/wgss.png)
![铂涛旅行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/plateno.png)
![乐心](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/lifesense.png)
![亿投传媒](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/reachmedia.png)
![股先生](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/guxiansheng.png)
![财学堂](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/caixuetang.png)
![4399](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/4399.png)
![汽车之家](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/autohome.png)
![面包财经](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mbcaijing.png)
![虎扑](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hoopchina.png)
![搜狐汽车](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/sohu-auto.png)
![量富征信](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/liangfuzhengxin.png)
![卖好车](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/maihaoche.png)
![中移物联网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zyiot.jpg)
![易车网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/biauto.png)
![一药网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/maiyaole.png)
![小影](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xiaoying.png)
![彩贝壳](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/caibeike.png)
![YEELIGHT](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yeelight.png)
![积目](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/itsgmu.png)
![极致医疗](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/acmedcare.png)
![金汇金融](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jinhui365.png)
![久柏易游](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/900etrip.png)
![小麦铺](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/24xiaomai.png)
![搜款网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/vvic.png)
![米庄理财](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mizlicai.png)
![贝吉塔网络科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/bjt.png)
![微盟](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/weimob.png)
![网易卡搭](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/kada.png)
![股书](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/kapbook.png)
![聚贸](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jumore.png)
![广联达bimface](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/bimface.png)
![环球易购](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/globalgrow.png)
![浙江执御](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jollychic.png)
![二维火](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/2dfire.jpg)
![上品](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/shopin.png)
![浪潮集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/inspur.png)
![纳里健康](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ngarihealth.png)
![橙红科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/oraro.png)
![龙腾出行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/dragonpass.png)
![荔枝](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/lizhi.fm.png)
![汇通达](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/htd.png)
![云融金科](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yunrong.png)
![天生掌柜](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/tszg360.png)
![容联光辉](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/rongplus.png)
![云天励飞](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/intellif.png)
![嘉云数据](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jiayundata.png)
![中泰证券网络金融部](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zts.png)
![网易易盾](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/163dun.png)
![享物说](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xiangwushuo.png)
![申通](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/sto.png) 
![金和网络](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jinhe.png) 
![二三四五](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/2345.png) 
![恒天财富](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/chtwm.jpg) 
![沐雪微信](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/uweixin.png) 
![温州医科大学附属眼视光医院](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/wzeye.png) 
![联通支付](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/10010pay.png) 
![杉数科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/shanshu.png) 
![分利宝](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/fenlibao.png) 
![核桃编程](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/hetao101.png) 
![小红书](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xiaohongshu.png) 
![幸福西饼](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/blissmall.png) 
![跨越速运](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ky-express.png) 
![OYO](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/oyohotels.png) 
![叮咚买菜](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/100-me.png) 
![智道网联](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zhidaohulian.jpg) 
![雪球](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xueqiu.jpg) 
![车通云](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/autocloudpro.png) 
![哒哒英语](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/dadaabc.png) 
![小E微店](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xedaojia.jpg) 
![达令家](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/daling.png) 
![人力窝](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/renliwo.png) 
![嘉美在线](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/mocire.jpg) 
![极易付](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/uepay.png) 
![智慧开源](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/wdom.png) 
![车仕库](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/cheshiku.png) 
![太美医疗科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/taimeitech.png) 
![亿联百汇](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yilianbaihui.png) 
![舟谱数据](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zhoupu123.png) 
![芙蓉兴盛](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/frxs.png) 
![野兽派](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/beastshop.png) 
![凯叔讲故事](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/kaishustory.png) 
![好大夫在线](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/haodf.png) 
![云幂信息技术](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/insyunmi.png) 
![兑吧](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/duiba.png) 
![九机网](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/9ji.png) 
![随手科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/sui.png) 
![万谷盛世](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/aixiangdao.png) 
![云账房](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yunzhangfang.png) 
![浙江远图互联](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yuantutech.png) 
![青客公寓](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/qk365.png) 
![东方财富](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/eastmoney.png) 
![极客修](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jikexiu.png) 
![美市科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/meix.png) 
![中通快递](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zto.png) 
![易流科技](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/e6yun.png) 
![实习僧](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/xiaoyuanzhao.png) 
![达令家](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/dalingjia.png) 
![寺库](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/secoo.png) 
![连连支付](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/lianlianpay.png) 
![众安保险](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/zhongan.png) 
![360金融](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/360jinrong.png) 
![中航服商旅](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/caschina.png) 
![贝壳](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/ke.png) 
![Yeahmobi易点天下](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/yeahmobi.png) 
![北京登云美业网络科技有限公司](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/idengyun.png) 
![金和网络](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/jinher.png) 
![中移（杭州）信息技术有限公司](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/komect.png) 
![北森](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/beisen.png) 
![合肥维天运通](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/log56.png) 
![北京蜜步科技有限公司](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/meboth.png) 
![术康](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/postop.png) 
![富力集团](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/rfchina.png) 
![天府行](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/tfxing.png) 
![八商山](https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/known-users/8travelpay.png) 

# Awards

<img src="https://raw.githubusercontent.com/ctripcorp/apollo-community/master/images/awards/oschina-2018-award.jpg" width="240px" alt="2018 年度最受欢迎中国开源软件">

# Stargazers over time

[![Stargazers over time](https://starcharts.herokuapp.com/ctripcorp/apollo.svg)](https://starcharts.herokuapp.com/ctripcorp/apollo)
