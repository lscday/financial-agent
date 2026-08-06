#!/bin/bash
# ============================================================
# LLM Code Review 脚本（GitHub Actions 触发）
# 每次 push/PR 到 main 时，用 DeepSeek 审查本次代码变更
# ============================================================
set -e

log()    { echo "[$(date '+%H:%M:%S')] $*"; }
log_ok() { echo "[$(date '+%H:%M:%S')] ✅ $*"; }
log_warn() { echo "[$(date '+%H:%M:%S')] ⚠️  $*"; }
log_err() { echo "[$(date '+%H:%M:%S')] ❌ $*"; }

log "========== LLM Code Review 开始 =========="

# ── 环境变量防御 ──
DEEPSEEK_API_KEY="${DEEPSEEK_API_KEY:-}"
BASE_SHA="${BASE_SHA:-}"
HEAD_SHA="${HEAD_SHA:-}"
REF_NAME="${REF_NAME:-main}"

# 保护：GitHub 首次 push 时 BASE_SHA 是 40 个 0，无法 diff，跳过
ZERO_SHA="0000000000000000000000000000000000000000"
if [ -z "$BASE_SHA" ] || [ "$BASE_SHA" = "$ZERO_SHA" ]; then
    log_warn "BASE_SHA 无效（首次推送或 PR 创建），跳过审查"
    exit 0
fi

# 保护：key 缺失则跳过（审查失败不阻塞流水线）
if [ -z "$DEEPSEEK_API_KEY" ]; then
    log_warn "DEEPSEEK_API_KEY 未配置，跳过审查"
    log_warn "请在 GitHub → Settings → Secrets and variables → Actions 添加 DEEPSEEK_API_KEY"
    exit 0
fi

# ── ① 获取本次 diff ──
log "步骤1: 获取 diff（${BASE_SHA} → ${HEAD_SHA}）..."
DIFF=$(git diff "${BASE_SHA}" "${HEAD_SHA}" -- '*.java' | head -c 40000)
DIFF_SIZE=$(echo "$DIFF" | wc -c | tr -d ' ')
CHANGED_FILES=$(git diff --name-only "${BASE_SHA}" "${HEAD_SHA}" -- '*.java' | head -10)

log "  diff 大小: ${DIFF_SIZE} 字节"
log "  变更 Java 文件: ${CHANGED_FILES}"

if [ -z "$DIFF" ]; then
    log "📭 本次无 Java 变更，跳过审查"
    exit 0
fi

# ── ② 提取变更文件上下文（前 8 个，每个前 4KB）──
log "步骤2: 提取文件上下文..."
CONTEXT=""
FILE_COUNT=0
for file in $CHANGED_FILES; do
    if [ $FILE_COUNT -ge 8 ]; then
        CONTEXT="${CONTEXT}
... 更多变更文件已省略（控制长度）"
        break
    fi
    if [ -f "$file" ]; then
        FILE_CONTENT=$(head -c 4000 "$file" 2>/dev/null)
        if [ -n "$FILE_CONTENT" ]; then
            FILE_COUNT=$((FILE_COUNT + 1))
            CONTEXT="${CONTEXT}
=== ${file} ===
${FILE_CONTENT}
"
        fi
    fi
done

# ── ③ 构造审查 Prompt ──
log "步骤3: 构造审查 Prompt..."

SYSTEM='你是资深的 Java 后端架构师，正在审查一个基于 LangChain4j + LangGraph4j 的金融 AI Agent 平台代码。

## 业务背景
这是金融资管领域的 AI 中台：多 Agent 工作流（LangGraph4j 状态图）+ RAG 语义检索 + 流式对话 + 文件存储。涉及资金清算、风险评估、产品查询等业务。代码错误可能导致资金损失或安全事故。

## 审查步骤（内部推理，不输出）
1. 只识别 diff 中实际新增/修改的行，不是上下文文件中的既有代码
2. 判断修改的业务意图
3. 逐条对照清单检查
4. 确认后输出；不确定标 [待确认]

## 审查清单（按优先级）

### [阻断] 资金安全 & 数据安全
- 金额/利率计算：BigDecimal 精度丢失、浮点直接计算、单位混淆（元/分）
- 密钥泄露：API key、密码、AccessKey 明文写进代码或配置
- 敏感数据：用户手机号/身份证等明文打印或存储

### [严重] 并发 & 稳定性
- 虚拟线程并发安全：共享可变状态未用锁/原子类；多线程用 HashMap 而非 ConcurrentHashMap
- 资源泄漏：Stream/IO/HTTP 连接未关闭（try-with-resources）
- LLM 调用异常：外部调用返回值未判空直接使用；无降级处理
- LangGraph 状态操作：AgentState 的 Map 取值类型转换错误

### [一般] 架构 & 代码质量
- DDD 分层违规：domain 层出现技术框架依赖（ES/RestClient/Spring MVC）；application 层出现业务规则 if/else；Controller 直接调 infrastructure
- 异常吞没：catch 块为空或只打日志不处理不抛出
- 魔法值：硬编码字符串/数字，未提取为常量或枚举
- 参数校验：入参缺少 @Valid/@NotBlank 校验

### [建议] 可维护性
- 重复代码、长方法可拆分、命名不规范

## 输出格式（严格）
每个问题之间空一行：

### [阻断] 文件:行号 - 问题简述
> 详情：具体风险描述
> 修复：具体可执行的修复方案

### [严重] 文件:行号 - 问题简述
> 详情：...
> 修复：...

### [一般] 文件:行号 - 问题简述
> 详情：...
> 修复：...

### [建议] 文件:行号 - 问题简述
> 详情：...
> 修复：...

未发现问题则只输出：
✅ 未发现代码问题

## 铁律
1. 只看 diff 新增行，不看上下文
2. 找不到问题就输出 ✅ 未发现代码问题，不编造
3. 行号必须真实，来自 diff 的 @@ 标记
4. 修复建议必须可执行，给具体代码
5. 不确定就标 [待确认]'

if [ -n "$CONTEXT" ]; then
    USER_MSG="以下是变更文件的完整内容（仅用于理解业务，不是审查对象）：

${CONTEXT}

════════════════════════════════════
⚠️ 以下是本次变更的 diff — 只审查这部分：
════════════════════════════════════

${DIFF}"
else
    USER_MSG="${DIFF}"
fi

# ── ④ 调用 DeepSeek API ──
log "步骤4: 调用 DeepSeek API..."

JSON_BODY=$(jq -n \
    --arg model "deepseek-chat" \
    --arg system "$SYSTEM" \
    --arg user "$USER_MSG" \
    '{model: $model, messages: [{role: "system", content: $system}, {role: "user", content: $user}], temperature: 0, max_tokens: 4096, stream: false}')

RESP_FILE=$(mktemp)
trap "rm -f '$RESP_FILE'" EXIT

HTTP_CODE=$(curl -s -w "%{http_code}" -o "$RESP_FILE" --max-time 180 \
    "https://api.deepseek.com/v1/chat/completions" \
    -H "Authorization: Bearer ${DEEPSEEK_API_KEY}" \
    -H "Content-Type: application/json" \
    -d "$JSON_BODY" | tr -d '\n\r ')

RESP_BODY=$(cat "$RESP_FILE")

if [ "$HTTP_CODE" != "200" ]; then
    log_err "DeepSeek API 返回 ${HTTP_CODE}"
    echo "$RESP_BODY" | head -c 500
    log_warn "审查失败，放行流水线（审查系统不阻塞开发）"
    exit 0
fi

CONTENT=$(echo "$RESP_BODY" | jq -r '.choices[0].message.content // empty' 2>/dev/null)
if [ -z "$CONTENT" ]; then
    log_warn "DeepSeek 返回空内容，放行流水线"
    exit 0
fi

# ── ⑤ 输出报告 + 严重性判定 ──
log "步骤5: 审查报告"
echo ""
echo "========================================"
echo "🤖 LLM Code Review Report"
echo "========================================"
echo "$CONTENT"
echo "========================================"

BLOCKER_COUNT=$(echo "$CONTENT" | grep -c '^### \[阻断\]' || echo 0)
MAJOR_COUNT=$(echo "$CONTENT" | grep -c '^### \[严重\]' || echo 0)
log "  阻断: ${BLOCKER_COUNT}, 严重: ${MAJOR_COUNT}"

FAIL_COUNT=$((BLOCKER_COUNT + MAJOR_COUNT))
if [ "$FAIL_COUNT" -gt 0 ]; then
    log_err "存在 ${BLOCKER_COUNT} 个阻断、${MAJOR_COUNT} 个严重问题！请修复后重新推送。"
    exit 1
fi

log_ok "无阻断/严重问题，审查通过"
exit 0
