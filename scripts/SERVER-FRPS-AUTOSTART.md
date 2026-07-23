# 服务器 frps 开机自启

只需要在阿里云服务器 SSH 里执行一次。作用：服务器重启后自动启动 `/www/server/frp/frps`，不再依赖手动 `nohup`。

## 1. 停掉当前手动启动的 frps

```bash
pkill -f '/www/server/frp/frps' || true
```

## 2. 创建 systemd 服务

```bash
cat > /etc/systemd/system/oa-frps.service <<'EOF'
[Unit]
Description=OA FRP Server
After=network.target

[Service]
Type=simple
WorkingDirectory=/www/server/frp
ExecStart=/www/server/frp/frps -c /www/server/frp/frps.toml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

## 3. 启用并启动

```bash
systemctl daemon-reload
systemctl enable --now oa-frps
systemctl status oa-frps --no-pager
```

## 4. 检查端口

```bash
ss -lntp | egrep '(:7000|:18090|:18091|:18092)\b' || true
```

正常情况下至少能看到 `7000` 和 `18090`。当 Windows 的 `frpc` 启动后，还会看到 `18091`、`18092`。

`18091` 只承载前端，`18092` 只承载网关。AI 请求由网关转发到 Windows 本机 `18083`，不要在 FRPS 中额外暴露 AI、Qdrant或 Ollama 端口。

## 常用命令

```bash
systemctl restart oa-frps
systemctl stop oa-frps
systemctl status oa-frps --no-pager
journalctl -u oa-frps -n 80 --no-pager
```
