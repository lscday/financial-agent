# 金融资管 AI 智能中台（financial-agent）项目规范

## 个人偏好

### 沟通风格
- 简洁直接，先结论后展开
- 技术讲解用后端视角，多给可运行示例

### 代码风格
- **显式类型，不使用 `var`** 局部变量写完整类型
- 禁 `@Autowired` 字段注入，用构造器注入
- Controller 入参引用 `application/command/` 里的 record，禁在 Controller 内定义 Request
- Command 用 record，禁 Map 传参
- 实体禁 `@Setter`，状态变更用行为方法
- Javadoc：类级注释说明职责，方法写 `@param/@return`

## 项目定位

基于 LangChain4j + LangGraph4j + Spring Boot 的**金融 AI Agent 平台**。
DDD 多模块架构，已完成 GitHub 接入 + LLM 代码审查。

## 模块地图

```
agentWork/  （Maven 父工程）
├── financial-agent-domain          领域层：实体/值对象/枚举/领域服务/仓储接口
├── financial-agent-application     应用层：用例编排/Command/Agent 图与节点
├── financial-agent-infrastructure  基础设施层：ES/文件存储/日志/LLM 工厂/模拟工具
├── financial-agent-interfaces      接口层：HTTP Controller 门面
└── financial-agent-starter         启动装配：启动类/配置/前端/文档
```

## 架构规范（DDD 分层）

- **依赖方向**：`infrastructure → interfaces → application → domain`（Maven 强制）
- **红线：业务规则只能写在 domain 层，application 只做编排**
- domain 不依赖任何技术框架（无 ES/SDK/Spring MVC）
- 仓储接口在 domain，实现在 infrastructure
- 端口-适配器模式：接口在 domain，本地/真云实现按 `@Profile` 切换
- 跨层调用靠 Maven 依赖杜绝（编译期拦截）

## 核心技术

- Java 21 + Spring Boot 3.4 + Maven 多模块
- LangChain4j 1.16（LLM）/ LangGraph4j 1.8（多 Agent 状态图）
- DeepSeek（chat）+ 阿里云百炼 text-embedding-v3（embedding）
- Elasticsearch 8.x（dense_vector kNN 检索）
- 虚拟线程 + Semaphore 信号量限流

## 智能 RAG 路由

- 检索后按最高相似度三档判断：
  - `≥ high-threshold(0.75)` → 直接 RAG
  - `< low-threshold(0.70)` → 纯 LLM
  - 中间档 → LLM 确认片段是否足以回答
- 阈值配置在 `application.yml` 的 `rag.route.*`
- 判断"是否足够"用 `confirm.trim().startsWith("够")` 精确匹配，勿用宽松 contains

## 密钥安全

- **API key 绝不写进代码/配置文件/提交 git**
- key 走环境变量 `${ENV:}`，如 `${DEEPSEEK_API_KEY:}`
- `application.yml`、`application-aliyun.yml` 已被 gitignore 排除
- 启动需配：`DEEPSEEK_API_KEY`、`TONGYI_API_KEY`

## 本地运行

- 需本地 ES 启动（`localhost:9200`，8.17 客户端兼容 9.4 服务端）
- 需配 `DEEPSEEK_API_KEY`、`TONGYI_API_KEY` 环境变量
- `cd financial-agent-starter && mvn spring-boot:run`
- 前端：`http://localhost:8080/`（单页，含 AI 对话/Agent 工作流/RAG/清算状态/文件存储）

## 阿里云接入（端口-适配器）

| 服务 | 状态 | 说明 |
|---|---|---|
| Elasticsearch | ✅ 已可对接 | 改配置 `elasticsearch.hosts` 指向阿里云 ES |
| OSS | ⏳ 待补实现 | 补 `@Profile("aliyun")` 的 `OssObjectStorageAdapter` |
| SLS | ⏳ 待补实现 | 补 `@Profile("aliyun")` 的 `SlsLogListener` |
| ECS | ⏳ 部署脚本已备好 | `script/Dockerfile` + `deploy.sh` |

- 本地实现：`@Profile("!aliyun")`（文件存本地/日志打控制台）
- 真云切换：`--spring.profiles.active=aliyun`
- 用户阿里云账号：中国站 `aliyun.com`

## GitHub 接入

- 仓库：`https://github.com/lscday/financial-agent`
- **LLM 代码审查**：`.github/workflows/code-review.yml` + `script/llm_review.sh`，push/PR 到 main 触发 DeepSeek 审查
- 审查 key 存 GitHub Secret `DEEPSEEK_API_KEY`
- 推 GitHub 需配代理：`git config http.proxy http://127.0.0.1:10808`
- 提交作者：`理世超 <583244214@qq.com>`

## 提交规范

- 用户说「提交」才执行 git commit，不主动提交
- commit 信息用中文，简述改动
- 涉及密钥/敏感文件的改动，先确认不泄露
