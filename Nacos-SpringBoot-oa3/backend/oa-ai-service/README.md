# OA AI Service

独立的 Spring Boot 3.5.16 + Spring AI 1.1.8 服务，已统一到后端同一 Spring Boot 基线。它从 MySQL `day.kb_doc` 加载知识，使用 Ollama `bge-m3` 和 Qdrant 完成语义检索，再由 `qwen2.5:7b` 生成回答。

## 设计原因

现在网关、员工端、管理端和 AI 服务都统一在同一个 Spring Boot 3.5.16 父 POM 下，避免了双基线维护。

## 主要能力

- Qdrant向量检索与关键词检索融合。
- SSE流式回答和普通 JSON回答。
- 员工令牌鉴权、内部重载令牌保护。
- 管理端知识修改后的版本化向量热重载。
- 会话条数、空闲过期和最大容量限制。
- Qdrant异常时自动降级到关键词检索。

## 依赖

- MySQL `day` 数据库
- Ollama `qwen2.5:7b`
- Ollama `bge-m3`
- Qdrant `127.0.0.1:6334`
- OA员工服务 `127.0.0.1:18081`

必须设置：

```text
OA_DB_PASSWORD
OA_INTERNAL_TOKEN
```

## 运行

```powershell
mvn spring-boot:run
```

健康检查：

```text
http://127.0.0.1:18083/api/v1/ai/chat/health
```

正常状态应包含：

```text
ollama=true
modelReady=true
ragReady=true
vectorReady=true
knowledgeCount=22
vectorCount=22
```

## 测试

```powershell
mvn test
mvn -Dtest=LiveRagAcceptanceIT test
```

第二条命令连接真实 MySQL、Ollama和独立 Qdrant验收 Collection，不使用 Mock知识库。
