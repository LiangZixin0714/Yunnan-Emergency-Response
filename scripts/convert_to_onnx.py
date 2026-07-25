"""将Embedding模型转换为ONNX格式以减少内存占用。"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sentence_transformers import SentenceTransformer
import onnxruntime as ort
import torch
import numpy as np

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "bge-small-zh-v1.5")
ONNX_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models", "bge-small-zh-v1.5-onnx")

os.makedirs(ONNX_DIR, exist_ok=True)

print(f"📂 源模型目录: {MODEL_DIR}")
print(f"📂 ONNX输出目录: {ONNX_DIR}")

print("\n🔄 正在加载模型...")
model = SentenceTransformer(MODEL_DIR, local_files_only=True)

print("\n🔄 正在导出为ONNX格式...")
dummy_input = ["测试文本"]
inputs = model.tokenize(dummy_input)

input_names = ["input_ids", "attention_mask"]
output_names = ["output"]

dynamic_axes = {
    "input_ids": {0: "batch_size", 1: "sequence_length"},
    "attention_mask": {0: "batch_size", 1: "sequence_length"},
    "output": {0: "batch_size"}
}

torch.onnx.export(
    model._first_module(),
    (inputs["input_ids"], inputs["attention_mask"]),
    os.path.join(ONNX_DIR, "model.onnx"),
    input_names=input_names,
    output_names=output_names,
    dynamic_axes=dynamic_axes,
    opset_version=13,
    do_constant_folding=True
)

print("\n📦 正在保存tokenizer...")
model.tokenizer.save_pretrained(ONNX_DIR)

print("\n✅ ONNX模型转换完成！")
print(f"   输出目录: {ONNX_DIR}")