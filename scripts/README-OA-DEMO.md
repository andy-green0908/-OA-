# Nexus Office 本地演示

## 组件与端口

| 端口 | 组件 | 公网暴露 |
|---:|---|---|
| 3306 | MySQL | 否 |
| 6379 | Redis | 否 |
| 6333/6334 | Qdrant | 否 |
| 8848 | Nacos | 否 |
| 9200 | Elasticsearch | 否 |
| 11434 | Ollama | 否 |
| 5173 | 前端 | 通过 FRP 18091 |
| 18080 | 网关 | 通过 FRP 18092 |
| 18081 | 员工服务 | 仅经过网关 |
| 18082 | 管理服务 | 仅经过网关 |
| 18083 | AI 服务 | 仅经过网关 |

## 前置条件

- Windows 用户环境中已设置 `OA_DB_PASSWORD` 和 `OA_INTERNAL_TOKEN`。
- Docker Desktop 已安装，并存在 `nexus-qdrant` 容器与 `nexus-qdrant-data` 卷。
- Ollama 已安装 `qwen2.5:7b` 和 `bge-m3`。
- 后端 JAR 和前端 `dist` 已构建。

## 启动

在 PowerShell 执行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\nexus-office\scripts\start-oa-demo.ps1
```

脚本按以下顺序启动或检查：

```text
MySQL / Redis / Nacos / Elasticsearch
Docker / Qdrant / Ollama
OA-2 / OA-7 / AI / Gateway
Frontend / FRP
```

脚本不会显示数据库密码或内部令牌，只检查它们是否存在。

## 状态检查

```powershell
powershell -ExecutionPolicy Bypass -File D:\nexus-office\scripts\status-oa-demo.ps1
```

除了端口状态，还会检查 Qdrant、Ollama、AI 向量状态和公网入口。

## 停止

只停止前端、四个后端服务和 FRP：

```powershell
powershell -ExecutionPolicy Bypass -File D:\nexus-office\scripts\stop-oa-demo.ps1
```

同时停止 Redis、Nacos、Elasticsearch、Qdrant和 Ollama：

```powershell
powershell -ExecutionPolicy Bypass -File D:\nexus-office\scripts\stop-oa-demo.ps1 -IncludeInfra
```

MySQL 和 Docker Desktop 本身不会被停止。

## FRP 拓扑

```text
公网 18091 -> 本机 5173 前端
公网 18092 -> 本机 18080 网关
网关 /api/v1/ai/** -> 本机 18083 AI 服务
```

不要为 18081、18082、18083、6333、6334、11434 新增公网映射。

公网访问地址：`http://8.148.22.63/`

服务器端 FRPS 自启动参见 `SERVER-FRPS-AUTOSTART.md`。
