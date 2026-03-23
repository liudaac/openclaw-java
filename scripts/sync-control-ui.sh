#!/bin/bash
#
# Control UI 同步脚本
#
# 从原版 OpenClaw 构建 UI 并同步到 Java 版的静态资源目录
#
# Usage: ./scripts/sync-control-ui.sh [openclaw-source-path]
#

set -e

# 配置
OPENCLAW_SOURCE="${1:-/root/openclaw}"
JAVA_STATIC_DIR="$(dirname "$0")/../openclaw-server/src/main/resources/static/control-ui"

echo "=== OpenClaw Control UI Sync ==="
echo "Source: $OPENCLAW_SOURCE/ui"
echo "Target: $JAVA_STATIC_DIR"
echo ""

# 检查源目录
if [[ ! -d "$OPENCLAW_SOURCE/ui" ]]; then
    echo "Error: UI source directory not found: $OPENCLAW_SOURCE/ui"
    echo "Usage: $0 [openclaw-source-path]"
    exit 1
fi

# 进入 UI 目录
cd "$OPENCLAW_SOURCE/ui"

# 检查 node_modules
if [[ ! -d "node_modules" ]]; then
    echo "Installing dependencies..."
    npm install
fi

# 构建 UI
echo "Building Control UI..."
npm run build

# 检查构建输出
if [[ ! -d "dist" ]]; then
    echo "Error: Build failed - dist directory not found"
    exit 1
fi

# 备份旧的静态资源
if [[ -d "$JAVA_STATIC_DIR" ]]; then
    BACKUP_DIR="${JAVA_STATIC_DIR}.backup.$(date +%Y%m%d-%H%M%S)"
    echo "Backing up old static resources to: $BACKUP_DIR"
    mv "$JAVA_STATIC_DIR" "$BACKUP_DIR"
fi

# 复制新的静态资源
echo "Copying new static resources..."
mkdir -p "$JAVA_STATIC_DIR"
cp -r dist/* "$JAVA_STATIC_DIR/"

# 修复 index.html 中的路径（如果需要）
if [[ -f "$JAVA_STATIC_DIR/index.html" ]]; then
    echo "Fixing index.html paths..."
    # 将绝对路径改为相对路径
    sed -i 's|src="/|src="./|g' "$JAVA_STATIC_DIR/index.html"
    sed -i 's|href="/|href="./|g' "$JAVA_STATIC_DIR/index.html"
fi

echo ""
echo "=== Sync Complete ==="
echo "New static resources:"
ls -la "$JAVA_STATIC_DIR"
echo ""
echo "Next steps:"
echo "1. Review the changes: git diff --stat"
echo "2. Test the UI: mvn spring-boot:run -pl openclaw-server"
echo "3. Commit the changes: git add openclaw-server/src/main/resources/static/control-ui"
