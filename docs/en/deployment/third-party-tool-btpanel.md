# aaPanel (Applicable versions 7.0.11 and above) Deployment guidelines

##  aaPanel  Docker  One-ClickInstall

<Steps>
  Go to [aaPanel official website](https://www.aapanel.com/new/download.html), Select the script to download and install
  (Skip this step if you already have it installed)

# Deploy Apollo using aaPanel

## Prerequisite

To install aaPanel, go to the [aaPanel](https://www.aapanel.com/new/download.html#install) official website and select the corresponding script to download and install.

## Deployment

aaPanel(Applicable versions 7.0.11 and above) Deployment guidelines

1. Log in to aaPanel and click `Docker` in the menu bar

   ![Docker](https://github.com/user-attachments/assets/a4aeb715-9e28-49b0-bb1d-3dd63d1394a2)

2. The first time you will be prompted to install the `Docker` and `Docker Compose` services, click Install Now. If it is already installed, please ignore it.

   ![install](https://github.com/user-attachments/assets/d48a83b7-eea4-43ec-a07e-bce6442c6fec)

3. After the installation is complete, find `Apollo` in `One-Click Install` and click `install`  

   ![install-Apollo](https://github.com/user-attachments/assets/e80dc7c0-e469-49b0-bd7f-225df36cdcab)

4. configure basic information such as the domain name, ports to complete the installation

   Note:
   The domain name is optional, if the domain name is filled, it can be managed through [Website]--> [Proxy Project], and you do not need to check [Allow external access] after filling in the domain name, otherwise you need to check it before you can access it through the port

   ![addApollo](https://github.com/user-attachments/assets/9623c6d9-d0e5-4f87-adea-e5a95133cf3c)

5. After installation, enter the domain name or IP+ port set in the previous step in the browser to access.

  - Name: application name, default `Apollo-characters`
  - Version selection: default `latest`
  - Domain name: If you need to access directly through the domain name, please configure the domain name here and resolve the domain name to the server
  - Allow external access: If you need direct access through `IP+Port`, please check. If you have set up a domain name, please do not check here.
  - Web port: Default `8070`, can be modified by yourself
  - Communication port: Default `8080`, can be modified by yourself
  - Metadata port: Default `8090`, can be modified by yourself

6. After submission, the panel will automatically initialize the application, which will take about `1-3` minutes. It can be accessed after the initialization is completed.


## Visit Apollo
- If you have set a domain name, please directly enter the domain name in the browser address bar, such as `http://demo.apollo.org`, to access the `Apollo` console.
- If you choose to access through `IP+Port`, please enter the domain name in the browser address bar to access `http://<aaPanelIP>:8070` to access the `Apollo` console.

![console](https://github.com/user-attachments/assets/6dd66f69-4c4d-4352-82aa-2eab0a5db33e)

> Default credentials: username `apollo`, password `admin`

