"""下载bge-small-zh-v1.5模型到本地。"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from huggingface_hub import snapshot_download

MODEL_REPO = "BAAI/bge-small-zh-v1.5"
MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "bge-small-zh-v1.5")

print(f"📦 正在下载模型: {MODEL_REPO}")
print(f"📂 目标目录: {MODEL_DIR}")

if os.path.exists(MODEL_DIR):
    import shutil
    shutil.rmtree(MODEL_DIR)

snapshot_download(
    repo_id=MODEL_REPO,
    local_dir=MODEL_DIR,
    local_dir_use_symlinks=False,
    ignore_patterns=["*.bin.index.json"]
)

print("\n✅ 模型下载完成！")
print(f"📂 文件列表:")
for root, dirs, files in os.walk(MODEL_DIR):
    for file in files:
        file_path = os.path.join(root, file)
        file_size = os.path.getsize(file_path) / (1024 * 1024)
        print(f"   - {file} ({file_size:.2f} MB)")