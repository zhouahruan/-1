#!/bin/bash
set -e

echo "=========================================="
echo "  ZHShop 官方客户端 - 正式包构建脚本"
echo "=========================================="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

# 检查 .env
if [ ! -f ".env" ]; then
    echo ">> 创建 .env 占位文件..."
    touch .env
fi

# 检查签名 Keystore
KEYSTORE_PATH="${KEYSTORE_PATH:-$ROOT_DIR/my-upload-key.jks}"
if [ ! -f "$KEYSTORE_PATH" ] && [ ! -f "$ROOT_DIR/debug.keystore" ]; then
    echo ">> 未找到签名文件，自动生成自签名密钥库..."
    keytool -genkey -v -keystore "$KEYSTORE_PATH" \
      -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
      -storepass android -keypass android \
      -dname "CN=ZHShop, OU=Mobile, O=ZHShop, L=Shenzhen, S=Guangdong, C=CN"
    export KEYSTORE_PATH="$KEYSTORE_PATH"
    export STORE_PASSWORD="android"
    export KEY_PASSWORD="android"
fi

echo ">> 开始编译正式版本 Release APK..."
if command -v gradle &> /dev/null; then
    gradle :app:assembleRelease
elif [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew :app:assembleRelease
else
    echo "❌ 未检测到 Gradle 构建工具！"
    exit 1
fi

echo "=========================================="
echo "✅ 正式包构建成功！输出路径:"
find app/build/outputs/apk/release -name "*.apk" 2>/dev/null || echo "app/build/outputs/apk/release/"
echo "=========================================="
