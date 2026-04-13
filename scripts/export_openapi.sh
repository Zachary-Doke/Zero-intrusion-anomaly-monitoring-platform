#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OPENAPI_URL="${OPENAPI_URL:-http://127.0.0.1:8080/v3/api-docs}"
OPENAPI_JSON="${OPENAPI_JSON:-$ROOT_DIR/docs/api/openapi.json}"
API_MARKDOWN="${API_MARKDOWN:-$ROOT_DIR/docs/api/API.md}"
GENERATOR_SCRIPT="$ROOT_DIR/scripts/generate_api_markdown.js"

mkdir -p "$(dirname "$OPENAPI_JSON")"
mkdir -p "$(dirname "$API_MARKDOWN")"

if ! command -v curl >/dev/null 2>&1; then
  echo "[docs] 未找到 curl，请先安装 curl。" >&2
  exit 1
fi

if ! command -v node >/dev/null 2>&1; then
  echo "[docs] 未找到 node，请先安装 Node.js。" >&2
  exit 1
fi

if [ ! -f "$GENERATOR_SCRIPT" ]; then
  echo "[docs] 未找到生成脚本: $GENERATOR_SCRIPT" >&2
  exit 1
fi

echo "[docs] 正在拉取 OpenAPI: $OPENAPI_URL"
if ! curl --noproxy '*' -fsS "$OPENAPI_URL" -o "$OPENAPI_JSON"; then
  echo "[docs] 拉取失败：请先启动 platform-server，或通过 OPENAPI_URL 指定可访问地址。" >&2
  echo "[docs] 参考启动命令: cd analyze-platform/platform-server && mvn spring-boot:run" >&2
  exit 1
fi

echo "[docs] 已写入: $OPENAPI_JSON"
node "$GENERATOR_SCRIPT" "$OPENAPI_JSON" "$API_MARKDOWN"

echo "[docs] 文档更新完成"
echo "[docs] - $OPENAPI_JSON"
echo "[docs] - $API_MARKDOWN"
