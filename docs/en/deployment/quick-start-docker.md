If you are familiar with Docker, you can use the Docker way to deploy Apollo quickly, so that you can quickly understand Apollo. if you are not very familiar with Docker, please refer to the [regular way to deploy Quick Start](en/deployment/quick-start).

If you are deploying Apollo in your company, please refer to the [distributed-deployment-guide](en/deployment/distributed-deployment-guide).

> Since Docker doesn't support windows very well, it is not recommended to use Docker way to deploy in windows environment, unless you know windows docker very well

## I. Preparation

### 1.1 Installing Docker
You can refer to [Docker Installation Guide](https://yeasy.gitbooks.io/docker_practice/content/install/) for the specific steps, and test whether the installation is successful with the following command
```
docker -v
```

To speed up Docker image download, it is recommended to [configure image gas pedal](https://yeasy.gitbooks.io/docker_practice/content/install/mirror.html).

### 1.2 Download Docker Quick Start configuration file

Download [docker-compose.yml](https://github.com/apolloconfig/apollo-quick-start/blob/master/docker-compose.yml) and [sql folder](https://github.com/apolloconfig/apollo-quick-start/tree/master/sql) to a local directory, such as docker-quick-start.

> If you are using a machine with an ARM architecture, such as a Mac M1, you will need to download [docker-compose-arm64.yml](https://github.com/apolloconfig/apollo-quick-start/blob/master/docker-compose-arm64.yml)

```bash
- docker-quick-start
  - docker-compose.yml
  - sql
    - apolloconfigdb.sql
    - apolloportaldb.sql
```

## II. Starting Apollo Configuration Center

Execute `docker-compose up` in the docker-quick-start directory, the first execution will trigger downloading images and other operations, you need to be patient and wait for some time.

> If you are using a machine with an ARM architecture, such as a Mac M1, execute `docker-compose -f docker-compose-arm64.yml up`

Search all the logs starting with `apollo-quick-start` and see the following logs indicating a successful start.
```log
apollo-quick-start | ==== starting service ====
apollo-quick-start | Service logging file is . /service/apollo-service.log
apollo-quick-start | Started [45]
apollo-quick-start | Waiting for config service startup .......
apollo-quick-start | Config service started. You may visit http://localhost:8080 for service status now!
apollo-quick-start | Waiting for admin service startup......
apollo-quick-start | Admin service started
apollo-quick-start | ==== starting portal ====
apollo-quick-start | Portal logging file is . /portal/apollo-portal.log
apollo-quick-start | Started [254]
apollo-quick-start | Waiting for portal startup .......
apollo-quick-start | Portal started. You can visit http://localhost:8070 now!
```

> Note 1: The database port is mapped to 13306, so if you want to access the database on the host, you can do so via localhost:13306, username is root and password is left blank.

> Note 2: If you want to view more service logs, you can log in via `docker exec -it apollo-quick-start bash`, then go to `/apollo-quick-start/service` and `/apollo-quick-start/portal` to view the log information.

## III. Using Apollo Configuration Center

You can refer to [Quick Start - IV. Using Apollo Configuration Center](en/deployment/quick-start?id=iv-using-apollo-configuration-center) for the steps related to using it.

Note that the Demo client needs to be run in a Docker environment with the following command.
```bash
docker exec -i apollo-quick-start /apollo-quick-start/demo.sh client
```

By default apollo-configservice will only register the internal IP, only clients started by the above command will be able to connect, if you want external clients to be able to access it, please refer to the [network policy](en/deployment/distributed-deployment-guide?id=_14-network-policy).