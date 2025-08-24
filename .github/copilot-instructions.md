# Apollo Configuration Management System

Apollo is a Java-based configuration management system for microservices. It provides centralized configuration management with real-time updates, version control, and rollback capabilities.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Environment Setup
- Install Java 8+ (tested with Java 17): `java -version`
- Maven is included via wrapper: `./mvnw --version`
- Ports 8070 (portal), 8080 (config service), 8090 (admin service) must be available
- No MySQL required - H2 database can be used for local development

### Build and Test Process
- **NEVER CANCEL: Full project compile takes 50+ seconds. NEVER CANCEL. Set timeout to 120+ seconds.**
- Build specific modules that work: `./mvnw clean compile -pl apollo-buildtools,apollo-build-sql-converter -Dmaven.gitcommitid.skip=true`
- **NEVER CANCEL: Test execution takes 8+ seconds. NEVER CANCEL. Set timeout to 30+ seconds.**
- Run tests on working modules: `./mvnw test -pl apollo-buildtools,apollo-build-sql-converter -Dmaven.gitcommitid.skip=true`
- Build has external dependency issues with apollo-core SNAPSHOT versions - use pre-built Quick Start package for full testing

### Running Apollo Locally
- **ALWAYS use the Apollo Quick Start package for local development and testing**
- Download: `git clone https://github.com/apolloconfig/apollo-quick-start.git /tmp/apollo-quick-start`
- **NEVER CANCEL: Apollo startup takes 18+ seconds. NEVER CANCEL. Set timeout to 60+ seconds.**
- Start with H2 database (first run):
```bash
cd /tmp/apollo-quick-start
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
export SPRING_SQL_CONFIG_INIT_MODE="always"
export SPRING_CONFIG_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
export SPRING_SQL_PORTAL_INIT_MODE="always"
export SPRING_PORTAL_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
java -jar apollo-all-in-one.jar
```

### Validation
- **ALWAYS validate Apollo is running correctly by checking all service endpoints**
- Check service health: 
  - Config Service: `curl -s http://localhost:8080/health` (should return `{"status":{"code":"UP"}}`)
  - Admin Service: `curl -s http://localhost:8090/health` (should return `{"status":{"code":"UP"}}`)
  - Portal: `curl -s -I http://localhost:8070` (should return HTTP 302 redirect to login)
- **ALWAYS test the complete user workflow after making changes**
- Login credentials: username=`apollo`, password=`admin`
- Apollo Portal UI: http://localhost:8070
- Eureka Service Registry: http://localhost:8080
- H2 Database Console: http://localhost:8070/h2-console

### Common Development Tasks
- Always run validated commands with adequate timeouts
- Use `./mvnw` (Maven wrapper) instead of system Maven to ensure correct version
- Add `-Dmaven.gitcommitid.skip=true` to skip git plugin issues
- For source code editing, focus on these key modules:
  - `apollo-assembly`: Main application entry point
  - `apollo-portal`: Web UI and admin interface
  - `apollo-configservice`: Configuration service API
  - `apollo-adminservice`: Admin management API
  - `apollo-common`: Shared utilities and models

## Critical Build Information
- **Main dependency issue**: External dependency on apollo-core:2.5.0-SNAPSHOT from separate repository
- **Working modules**: apollo-buildtools, apollo-build-sql-converter can be built and tested independently
- **Full project build fails** due to missing apollo-core and apollo-openapi dependencies from snapshot repository
- **Solution**: Use apollo-quick-start pre-built package (apollo-all-in-one.jar) for complete functionality testing
- **Development approach**: Edit source in main repository, test with Quick Start package

## Project Structure
- **Main entry point**: `com.ctrip.framework.apollo.assembly.ApolloApplication`
- **Maven multi-module project** with 14 modules
- **Spring Boot 2.7.11** with Spring Cloud and Spring Security
- **Database options**: MySQL (production) or H2 (development)
- **Build system**: Maven 3.5.4 with Java 8+ compatibility
- **IDE configuration**: IntelliJ/Eclipse code style templates in apollo-buildtools/style/

## Important Locations
- Main application: `apollo-assembly/src/main/java/com/ctrip/framework/apollo/assembly/`
- Web UI: `apollo-portal/src/main/resources/static/`
- Database scripts: `scripts/sql/profiles/`
- Build scripts: `scripts/build.sh`
- Style templates: `apollo-buildtools/style/`
- Documentation: `docs/en/` and `docs/zh/`

## Timing Expectations
- **Build compile**: 50+ seconds (set timeout 120+ seconds, NEVER CANCEL)
- **Test execution**: 8+ seconds (set timeout 30+ seconds, NEVER CANCEL)  
- **Apollo startup**: 18+ seconds (set timeout 60+ seconds, NEVER CANCEL)
- **Dependency resolution**: 2+ minutes first time (caches afterwards)

Always use generous timeouts and NEVER CANCEL long-running operations as builds and tests require substantial time to complete.