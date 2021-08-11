### 使用Shell Script读取Apollo的配置
```bash
#修改为需要读取的appid
appid="app1"
#修改为需要读取的config server
host="http://apollo-service-dev.svc.cluster.local:8080"
#cluster = default, namespace = application，按需修改
path="/configfiles/json/${appid}/default/application"
#该ENV配置的访问密钥
key=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
ts=$(echo $(($(date +%s)*1000)))
echo ">>> TS is ${ts}"
sign=$(printf "%s\n%s" ${ts} ${path})
sig=$(printf "%s\n%s" ${ts} ${path} | openssl dgst -hmac ${key}  -sha1 -binary | base64)
echo ">>> Sig=${sig}"
curl -H "Authorization:Apollo $appid:$sig"  \
     -H "Timestamp: ${ts}" \
     -H 'Content-Type:application/json;charset=UTF-8'  \
     $host${path}
echo ""
# 后续处理，可以把curl拿到的config写入一个文件，等等
```
