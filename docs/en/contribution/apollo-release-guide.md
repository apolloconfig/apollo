# 1. Upgrade version

Replace the version number in full under the Apollo project, e.g. 1.9.1-SNAPSHOT to 1.9.1. Be sure to replace the non-version number in the process.

# 2. Writing Release Reports

Each PR between two versions is recorded in [https://github.com/apolloconfig/apollo/blob/master/CHANGES.md](https://github.com/apolloconfig/apollo/blob/master/CHANGES.md), so you only need to extract it from CHANGES.md to write the release report. Release report reference: [https://github.com/apolloconfig/apollo/releases/tag/v1.8.0](https://github.com/apolloconfig/apollo/releases/tag/v1.8.0)

# 3. Version validation

Version validation consists of three main areas of validation.

1. Verification of newly introduced code changes, such as new features, bugfixes
1. Apollo core main process validation, including: configuration release, dynamic push, grayscale push, etc.
1. upgrade process validation, including: classic deployment mode, docker mode, k8s mode

# 4. Version release

## 4.1 Tagging

1. Pull the latest code from master
   1. git pull origin master  
2. Tag
   1. git tag ${new-version}
3. Push tag
   1. git push origin tag ${new-version}

## 4.2 Packages

### 4.2.1 Pre-checking

Before packaging, check your local environment, mvn -v to make sure the java version is 1.8, e.g. the following output.

> mvn -v
> Apache Maven 3.8.1 (05c21c65bdfed0f71a2f2ada8b84da59348c4c5d) Maven home: /usr/local/Cellar/maven/3.8.1/libexec
> Java version: 1.8.0_301, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/jdk1.8.0_301.jdk/Contents/Home/jre
> Default locale: zh_CN, platform encoding: UTF-8
> OS name: "mac os x", version: "10.16", arch: "x86_64", family: "mac"

### 4.2.2 Packages

In the ${apollo_home}/scripts/ directory, execute.

> . /build.sh

If the following error is reported.

> zsh: . /build.sh: bad interpreter: /bin/sh^M: no such file or directory

Then you need to execute the following command to convert to unix

> brew install dos2unix
> dos2unix build.sh

### 4.2.3 Calculating checksum for build packages

To calculate configservice checksum

> In the ${apollo_home}/apollo-configservice/target/ directory, execute.
>
> shasum apollo-configservice-${new-version}-github.zip > apollo-configservice-${new-version}-github.zip.sha1

Calculate adminservice checksum

> In the ${apollo_home}/apollo-adminservice/target/ directory, execute.
>
> shasum apollo-adminservice-${new-version}-github.zip > apollo-adminservice-${new-version}-github.zip.sha1

Calculate portal checksum

> In the ${apollo_home}/apollo-portal/target/ directory, execute.  
>
> shasum apollo-portal-${new-version}-github.zip > apollo-portal-${new-version}-github.zip.sha1

## 4.3 Creating a pre-release

Github create pre-release

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/create-release.png)

Fill out the Release Note & upload the package

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/fill-release-form.png)

## 4.4 Pre-release Apollo-Client Jar Package

Publish via github workflow.

[https://github.com/apolloconfig/apollo-java/actions/workflows/release.yml](https://github.com/apolloconfig/apollo-java/actions/workflows/release.yml)

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/publish-sdk.png)

> Note: If you are releasing a SNAPSHOT version, use the default value snapshots. If you are releasing an official version (without SNAPSHOT), you need to change it to releases to make it work.

## 4.5 Release PMC Polling

Polling is used to allow PMC members to collaborate on verifying the content of a release and preventing problematic releases.
The polling takes the form of a thread in Discussions, available at [https://github.com/apolloconfig/apollo/discussions/3899](https://github.com/apolloconfig/apollo/discussions/3899)

## 4.6 Official release of Apollo-Client Jar to repositories

## 4.7 Release Docker image

## 4.7.1 Build the image locally and test it

With the package packed in step 4.2, execute in the apollo root directory

> mvn docker:build -pl apollo-configservice,apollo-adminservice,apollo-portal

Note: If you get an error, you may need to restart the local docker

### 4.7.2 Push image to repository

Publish via github workflow.

[https://github.com/apolloconfig/apollo/actions/workflows/docker-publish.yml](https://github.com/apolloconfig/apollo/actions/workflows/docker-publish.yml)

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/publish-docker.jpg)

## 4.8 Update helm chart

### 4.8.1 Update the contents of the chart

1. cd [apollo-helm-chart](https://github.com/apolloconfig/apollo-helm-chart)
2. helm package apollo-portal && helm package apollo-service
3. mv *.tgz docs
4. cd docs
5. helm repo index .

### 4.8.2 Merging branches into main

Create a pull request to merge the above products into the [main](https://github.com/apolloconfig/apollo-helm-chart) branch.

# 5. Release announcement

Reference: [https://github.com/apolloconfig/apollo/discussions/3740](https://github.com/apolloconfig/apollo/discussions/3740)
