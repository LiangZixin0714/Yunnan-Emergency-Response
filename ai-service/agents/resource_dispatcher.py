"""资源调度Agent，根据灾情位置/类型，调用后端接口查可用资源。"""

import logging
import sys
import os
from typing import Dict, List, Any

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger

logger = setup_logger()


def dispatch_resources(info: Dict[str, Any]) -> List[Dict[str, Any]]:
    """根据灾情信息调度可用资源。
    
    Args:
        info: 包含灾情类型、等级、位置等信息的字典
        
    Returns:
        推荐资源列表，每个资源包含类型、数量、位置等信息
    """
    logger.info(f"🚚 开始资源调度，灾害类型: {info.get('type')}, 位置: {info.get('location')}")
    
    try:
        # TODO: Day 3任务 - 调用后端API查询可用资源
        # 目前返回模拟数据作为占位符
        
        disaster_type = info.get('type', '其他')
        location = info.get('location', '未知地点')
        level = info.get('level', '中')
        
        # 根据灾害类型生成推荐资源
        resource_templates = {
            "地震": [
                {"type": "team", "name": "地震救援队", "quantity": 100, "location": location},
                {"type": "medical", "name": "急救药品", "quantity": 500, "location": location},
                {"type": "vehicle", "name": "救护车", "quantity": 20, "location": location},
                {"type": "shelter", "name": "救灾帐篷", "quantity": 200, "location": location},
            ],
            "滑坡": [
                {"type": "team", "name": "山体救援队", "quantity": 80, "location": location},
                {"type": "vehicle", "name": "工程抢险车", "quantity": 10, "location": location},
                {"type": "medical", "name": "急救药品", "quantity": 300, "location": location},
            ],
            "洪涝": [
                {"type": "team", "name": "水域救援队", "quantity": 60, "location": location},
                {"type": "vehicle", "name": "冲锋舟", "quantity": 15, "location": location},
                {"type": "shelter", "name": "临时安置房", "quantity": 100, "location": location},
                {"type": "medical", "name": "消毒用品", "quantity": 200, "location": location},
            ],
            "干旱": [
                {"type": "vehicle", "name": "运水车", "quantity": 20, "location": location},
                {"type": "team", "name": "抗旱工作队", "quantity": 50, "location": location},
            ],
            "森林火灾": [
                {"type": "team", "name": "森林消防救援队", "quantity": 200, "location": location},
                {"type": "vehicle", "name": "消防车", "quantity": 30, "location": location},
                {"type": "vehicle", "name": "直升机", "quantity": 2, "location": location},
            ],
            "泥石流": [
                {"type": "team", "name": "泥石流救援队", "quantity": 100, "location": location},
                {"type": "vehicle", "name": "工程抢险车", "quantity": 15, "location": location},
                {"type": "shelter", "name": "救灾帐篷", "quantity": 150, "location": location},
            ],
        }
        
        resources = resource_templates.get(disaster_type, [
            {"type": "team", "name": "应急救援队", "quantity": 50, "location": location},
            {"type": "medical", "name": "急救药品", "quantity": 200, "location": location},
        ])
        
        # 根据灾害等级调整资源数量
        if level == "高":
            for resource in resources:
                resource["quantity"] = int(resource["quantity"] * 1.5)
        elif level == "低":
            for resource in resources:
                resource["quantity"] = int(resource["quantity"] * 0.7)
        
        logger.info(f"✅ 资源调度完成，推荐资源数: {len(resources)}")
        return resources
        
    except Exception as e:
        logger.error(f"❌ 资源调度失败: {e}")
        return []


# TODO: Day 3 任务 - 实现真实的后端API调用
# def _call_backend_api(endpoint: str, params: Dict[str, Any]) -> Any:
#     """调用后端API。
#     
#     Args:
#         endpoint: API端点
#         params: 请求参数
#         
#     Returns:
#         API响应数据
#     """
#     # 需要配置后端服务地址
#     # BACKEND_URL = os.environ.get("BACKEND_URL", "http://localhost:8080")
#     # 使用 requests 或 httpx 调用后端接口
#     pass


if __name__ == "__main__":
    # 测试资源调度功能
    test_info = {
        "type": "地震",
        "level": "高",
        "location": "云南省昆明市五华区",
        "affected_population": 500,
        "confidence": 0.9
    }
    
    result = dispatch_resources(test_info)
    print("资源调度结果:")
    for resource in result:
        print(f"- {resource['name']}: {resource['quantity']} 单位")