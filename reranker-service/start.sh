#!/usr/bin/env bash
# 启动本地重排服务
#
# 首次运行会创建虚拟环境并下载模型（约 2.3GB，需要一些时间）。
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -d ".venv" ]; then
    echo "==> 创建虚拟环境"
    python3 -m venv .venv
    ./.venv/bin/pip install --upgrade pip
    ./.venv/bin/pip install -r requirements.txt
fi

# MPS 尚未实现全部算子，允许未支持的算子回落到 CPU，避免直接报错
export PYTORCH_ENABLE_MPS_FALLBACK=1

echo "==> 启动重排服务 (http://127.0.0.1:${RERANK_PORT:-8081})"
exec ./.venv/bin/python app.py
