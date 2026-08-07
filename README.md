# 金融资管 AI 智能中台（Financial Agent）

基于 **LangChain4j + LangGraph4j + Spring Boot** 的金融 AI Agent 平台，采用 **DDD 多模块架构**，实现多 Agent 工作流、RAG 语义检索、流式智能对话与文件存储，并预留了阿里云接入点。

---

## ✨ 功能特性

### 1. 多 Agent 工作流（LangGraph4j 状态图）
基于 LangGraph4j 的状态图编排，覆盖完整的金融咨询流程：

```
START → 规则检索 → 风险检查
        ├── 高风险 → 分析 → END
        └── 正常 → 数据查询 → 清算
             ├── 失败且重试 < 3 → 回清算（循环）
             └── 成功 或 重试 ≥ 3 → 分析/END
```

- **规则检索**：从向量库检索相关金融政策/规则
- **风险检查**：LLM 评估风险等级（高风险/中风险/低风险）
- **数据查询**：产品信息查询（领域仓储）
- **资金清算**：模拟清算 + 最多 3 次重试
- **分析报告**：LLM 生成分析报告

### 2. 智能 RAG 语义检索（方法 C 路由）
升级为智能检索路由，根据检索相似度三档判断：

```
问题 → 检索知识库 → 取最高相似度 maxScore
  ├── maxScore ≥ 0.75 → 直接 RAG（引用文档回答）
  ├── maxScore < 0.70 → 纯 LLM（无关问题直接大模型兜底）
  └── 中间档 → LLM 确认片段是否足以回答，再决定 RAG 或纯 LLM
```

### 3. 流式对话（SSE）
- 流式输出（Server-Sent Events），打字机效果
- 智能路由：知识库命中的问题带来源引用，未命中的交给大模型自由发挥

### 4. 文件存储（端口-适配器）
- 上传/下载文件
- 接口抽象 `ObjectStorage`，本地实现存 `data/objects/`，可切换阿里云 OSS

### 5. 清算状态查询
- `GET /api/agent/settlement/{id}` 按清算编号查询状态

### 6. 前端页面
- 单页前端（AI 对话 / 多 Agent 工作流 / RAG 检索 / 清算状态 / 文件存储）

---

## 🛠 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | **Java 21** |
| 框架 | **Spring Boot 3.4** |
| AI | **LangChain4j 1.16**（LLM 调用）、**LangGraph4j 1.8**（多 Agent 状态图） |
| 大模型 | **DeepSeek**（chat）、**阿里云百炼 text-embedding-v3**（embedding） |
| 向量库 | **Elasticsearch 8.x**（dense_vector kNN 检索） |
| 并发 | **虚拟线程** + **Semaphore 信号量限流** |
| 构建 | **Maven 多模块** |
| 前端 | 原生 HTML/CSS/JS |

---

## 🏗 架构（DDD 多模块）

```
agentWork/
├── financial-agent-domain          # 领域层：实体/值对象/枚举/领域服务/仓储接口（业务规则唯一住所）
├── financial-agent-application     # 应用层：用例编排/Command/Agent 图与节点
├── financial-agent-infrastructure  # 基础设施层：ES 存储/文件存储/日志/LLM 工厂
├── financial-agent-interfaces      # 接口层：HTTP Controller 门面
└── financial-agent-starter         # 启动装配：启动类/配置/文档/前端
```

**依赖方向**：`infrastructure → interfaces → application → domain`（Maven 强制，跨层调用在编译期杜绝）

```
domain（纯接口 + 业务规则）
  ├── repository/ObjectStorage       ← 文件存储端口
  ├── repository/AuditLogPort        ← 审计日志端口
  ├── repository/EmbeddedVectorRepository ← 向量存储抽象
  └── service/AdvisoryWorkflowPolicy ← 工作流路由规则

infrastructure（本地回退实现）
  ├── LocalObjectStorageAdapter      ← 文件存本地 data/objects/
  ├── ConsoleLogListener             ← 日志打控制台
  └── es/EsEmbeddedVectorRepository  ← ES kNN 检索
```

---

## ☁️ 阿里云对接情况

采用**端口（接口）+ 适配器（实现）**模式，通过 Spring Profile 切换本地实现与真云实现。

### 对接状态总览

| 阿里云服务 | 用途 | 状态 | 说明 |
|---|---|---|---|
| **Elasticsearch** | RAG 向量检索 | ✅ **已可对接** | 只需改配置指向阿里云 ES 实例 |
| **OSS** | 文件存储 | ⏳ 接口已留好，真云实现待补 | 补 `OssObjectStorageAdapter` |
| **SLS** | 日志分析 | ⏳ 接口已留好，真云实现待补 | 补 `SlsLogListener` |
| **ECS** | 项目部署 | ⏳ 部署脚本已备好 | `script/Dockerfile` + `deploy.sh` |

### 切换机制（Profile 控制）

```java
// 本地实现：非 aliyun 环境生效（默认）
@Component
@Profile("!aliyun")
public class LocalObjectStorageAdapter implements ObjectStorage { ... }

// 真云实现：aliyun 环境生效（待补）
@Component
@Profile("aliyun")
public class OssObjectStorageAdapter implements ObjectStorage { ... }
```

- **本地运行**（默认）：文件存本地、日志打控制台、ES 连本地 localhost
- **接入真云**：`--spring.profiles.active=aliyun`，自动切换

### ES 对接（已支持，改配置即可）

`EsEmbeddedVectorRepository` 用 RestClient，`ElasticsearchConfig` 已支持用户名密码认证。只需在 `application-aliyun.yml` 配置：

```yaml
elasticsearch:
  hosts: https://es-cn-xxxx.aliyuncs.com:9200   # 阿里云 ES 地址
  username: elastic
  password: ${ALIYUN_ES_PASSWORD}
```

> 前提：把本机公网 IP 加入阿里云 ES 白名单。

### OSS / SLS 对接（待补实现）

接口已在 domain 层定义好（`ObjectStorage` / `AuditLogPort`），补齐 `@Profile("aliyun")` 的真云实现 + 对应 SDK 依赖即可，业务代码零改动。

---

## 🚀 快速启动

### 环境准备

| 依赖 | 说明 |
|---|---|
| JDK 21 | 必须 |
| Maven 3.9+ | 必须 |
| Elasticsearch | RAG 检索需要（本地或阿里云） |
| DeepSeek / 百炼 key | 环境变量注入 |

### 配置环境变量

```bash
# DeepSeek（对话/分析）
export DEEPSEEK_API_KEY=your-deepseek-key

# 阿里云百炼（文本向量化）
export TONGYI_API_KEY=your-tongyi-key
```

### 本地运行

```bash
# 构建
mvn clean package -DskipTests

# 启动（默认 profile，本地 ES）
cd financial-agent-starter
mvn spring-boot:run

# 或直接跑 jar
java -jar target/financial-agent-starter-1.0.0.jar
```

### 访问

浏览器打开 `http://localhost:8080/`（前端页面）

---

## 🔌 API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/agent/execute` | 执行多 Agent 工作流 |
| GET | `/api/agent/settlement/{id}` | 查询清算状态 |
| GET | `/api/agent/status` | 存活检查 |
| GET | `/api/chat/stream?question=` | 智能流式对话（SSE） |
| POST | `/api/chat/ask` | 非流式对话 |
| GET | `/api/rag/query?question=` | RAG 检索问答 |
| POST | `/api/file/upload` | 文件上传 |
| GET | `/api/file/download/**` | 文件下载 |

---
