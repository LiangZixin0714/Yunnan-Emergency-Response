"""资源调度Agent，根据灾情位置/类型，从数据库查询可用资源。"""

import logging
import sys
import os
import json
import pymysql
from typing import Dict, List, Any

# 添加项目根目录到sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logger import setup_logger

logger = setup_logger()

# MySQL 数据库配置（根据 db-schema.md，资源表在 MySQL emergency_db 中）
MYSQL_HOST = os.environ.get("MYSQL_HOST", "localhost")
MYSQL_PORT = int(os.environ.get("MYSQL_PORT", "3306"))
MYSQL_DATABASE = os.environ.get("MYSQL_DATABASE", "emergency_db")
MYSQL_USER = os.environ.get("MYSQL_USER", "root")
MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "ZAQ12wsx581!")

# 灾害类型与资源类型映射（根据 db-schema.md 数据字典）
DISASTER_RESOURCE_MAP = {
    "地震": ["设备", "人员", "物资", "医疗", "生活"],
    "滑坡": ["设备", "人员", "物资", "医疗", "生活"],
    "洪涝": ["设备", "人员", "物资", "医疗", "生活"],
    "干旱": ["设备", "人员", "物资", "医疗", "生活"],
    "森林火灾": ["设备", "人员", "物资", "医疗"],
    "泥石流": ["设备", "人员", "物资", "医疗", "生活"],
    "火灾": ["设备", "人员", "物资", "医疗"],
    "台风": ["设备", "人员", "物资", "医疗", "生活"],
}


def _get_db_connection():
    """获取 MySQL 数据库连接。"""
    try:
        import pymysql
        
        conn = pymysql.connect(
            host=MYSQL_HOST,
            port=MYSQL_PORT,
            database=MYSQL_DATABASE,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            connect_timeout=5,
            charset='utf8mb4'
        )
        logger.info(f"✅ MySQL数据库连接成功，数据库: {MYSQL_DATABASE}")
        return conn
    except ImportError:
        logger.error("❌ pymysql 库未安装，请先安装: pip install pymysql")
        return None
    except Exception as e:
        logger.error(f"❌ MySQL连接失败: {e}")
        return None


def _query_resources_by_disaster(disaster_type: str, limit: int = 5) -> List[Dict[str, Any]]:
    """根据灾害类型查询可用资源（从 MySQL emergency_resource 表）。"""
    conn = _get_db_connection()
    if not conn:
        return []
    
    resources = []
    
    try:
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        
        # 获取灾害对应的资源类型
        resource_types = DISASTER_RESOURCE_MAP.get(disaster_type, ["物资"])
        
        # 构建查询（查询可用状态且有可用库存的资源）
        type_placeholders = ",".join(["%s"] * len(resource_types))
        query = f"""
            SELECT id, resource_id, resource_name, resource_type, 
                   total_stock, available_stock, locked_stock, location, unit, status
            FROM emergency_resource
            WHERE status = 'available'
              AND available_stock > 0
              AND resource_type IN ({type_placeholders})
            ORDER BY available_stock DESC
            LIMIT %s;
        """
        
        cursor.execute(query, (*resource_types, limit))
        rows = cursor.fetchall()
        
        for row in rows:
            resources.append({
                "id": row["id"],
                "resource_id": row["resource_id"],
                "name": row["resource_name"],
                "type": row["resource_type"],
                "quantity": row["available_stock"],
                "total_stock": row["total_stock"],
                "locked_stock": row["locked_stock"],
                "location": row["location"] if row["location"] else "未知位置",
                "unit": row["unit"] if row["unit"] else "",
                "status": row["status"],
                "priority": "高" if row["available_stock"] > 100 else "中"
            })
        
        cursor.close()
        logger.info(f"🔍 数据库查询完成，返回 {len(resources)} 条资源")
        
    except Exception as e:
        logger.error(f"❌ 数据库查询失败: {e}")
        resources = []
    finally:
        if conn:
            conn.close()
    
    return resources


def _generate_recommended_resources(disaster_type: str, location: str, level: str) -> List[Dict[str, Any]]:
    """生成推荐资源列表（当数据库查询失败时使用）。"""
    resource_templates = {
        "地震": [
            {"type": "人员", "name": "地震救援队", "quantity": 100, "location": location, "priority": "高", "unit": "人"},
            {"type": "物资", "name": "急救药品", "quantity": 500, "location": location, "priority": "高", "unit": "箱"},
            {"type": "设备", "name": "救护车", "quantity": 20, "location": location, "priority": "高", "unit": "辆"},
            {"type": "物资", "name": "救灾帐篷", "quantity": 200, "location": location, "priority": "中", "unit": "顶"},
            {"type": "设备", "name": "工程抢险车", "quantity": 15, "location": location, "priority": "中", "unit": "辆"},
        ],
        "滑坡": [
            {"type": "人员", "name": "山体救援队", "quantity": 80, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "工程抢险车", "quantity": 10, "location": location, "priority": "高", "unit": "辆"},
            {"type": "物资", "name": "急救药品", "quantity": 300, "location": location, "priority": "中", "unit": "箱"},
            {"type": "物资", "name": "救灾帐篷", "quantity": 100, "location": location, "priority": "中", "unit": "顶"},
        ],
        "洪涝": [
            {"type": "人员", "name": "水域救援队", "quantity": 60, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "冲锋舟", "quantity": 15, "location": location, "priority": "高", "unit": "艘"},
            {"type": "物资", "name": "临时安置房", "quantity": 100, "location": location, "priority": "中", "unit": "套"},
            {"type": "物资", "name": "消毒用品", "quantity": 200, "location": location, "priority": "中", "unit": "箱"},
            {"type": "设备", "name": "运水车", "quantity": 10, "location": location, "priority": "低", "unit": "辆"},
        ],
        "干旱": [
            {"type": "设备", "name": "运水车", "quantity": 20, "location": location, "priority": "高", "unit": "辆"},
            {"type": "人员", "name": "抗旱工作队", "quantity": 50, "location": location, "priority": "中", "unit": "人"},
            {"type": "设备", "name": "抗旱设备", "quantity": 50, "location": location, "priority": "中", "unit": "台"},
        ],
        "森林火灾": [
            {"type": "人员", "name": "森林消防救援队", "quantity": 200, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "消防车", "quantity": 30, "location": location, "priority": "高", "unit": "辆"},
            {"type": "设备", "name": "直升机", "quantity": 2, "location": location, "priority": "高", "unit": "架"},
            {"type": "物资", "name": "急救药品", "quantity": 200, "location": location, "priority": "中", "unit": "箱"},
        ],
        "泥石流": [
            {"type": "人员", "name": "泥石流救援队", "quantity": 100, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "工程抢险车", "quantity": 15, "location": location, "priority": "高", "unit": "辆"},
            {"type": "物资", "name": "救灾帐篷", "quantity": 150, "location": location, "priority": "中", "unit": "顶"},
            {"type": "物资", "name": "急救药品", "quantity": 300, "location": location, "priority": "中", "unit": "箱"},
        ],
        "火灾": [
            {"type": "人员", "name": "消防救援队", "quantity": 100, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "消防车", "quantity": 20, "location": location, "priority": "高", "unit": "辆"},
            {"type": "物资", "name": "急救药品", "quantity": 200, "location": location, "priority": "中", "unit": "箱"},
        ],
        "台风": [
            {"type": "人员", "name": "应急救援队", "quantity": 80, "location": location, "priority": "高", "unit": "人"},
            {"type": "设备", "name": "冲锋舟", "quantity": 10, "location": location, "priority": "高", "unit": "艘"},
            {"type": "物资", "name": "临时安置点", "quantity": 150, "location": location, "priority": "中", "unit": "个"},
            {"type": "物资", "name": "急救药品", "quantity": 300, "location": location, "priority": "中", "unit": "箱"},
        ],
    }
    
    resources = resource_templates.get(disaster_type, [
        {"type": "人员", "name": "应急救援队", "quantity": 50, "location": location, "priority": "高", "unit": "人"},
        {"type": "物资", "name": "急救药品", "quantity": 200, "location": location, "priority": "中", "unit": "箱"},
    ])
    
    # 根据灾害等级调整资源数量
    if level == "高":
        for resource in resources:
            resource["quantity"] = int(resource["quantity"] * 1.5)
    elif level == "低":
        for resource in resources:
            resource["quantity"] = int(resource["quantity"] * 0.7)
    
    return resources


def dispatch_resources(info: Dict[str, Any]) -> List[Dict[str, Any]]:
    """根据灾情信息调度可用资源。
    
    Args:
        info: 包含灾情类型、等级、位置等信息的字典
        
    Returns:
        推荐资源列表，每个资源包含类型、数量、位置等信息
    """
    logger.info(f"🚚 开始资源调度，灾害类型: {info.get('type')}, 位置: {info.get('location')}, 等级: {info.get('level')}")
    
    disaster_type = info.get('type', '其他')
    location = info.get('location', '未知地点')
    level = info.get('level', '中')
    
    try:
        # 1. 尝试从 MySQL 数据库查询资源
        db_resources = _query_resources_by_disaster(disaster_type, limit=15)
        
        if db_resources:
            logger.info(f"✅ 资源调度完成，从数据库获取 {len(db_resources)} 条资源")
            return db_resources
        
        # 2. 数据库查询失败或无数据，使用模拟数据
        logger.warning("⚠️ 数据库无可用资源，使用推荐资源")
        resources = _generate_recommended_resources(disaster_type, location, level)
        
        logger.info(f"✅ 资源调度完成，生成 {len(resources)} 条推荐资源")
        return resources
        
    except Exception as e:
        logger.error(f"❌ 资源调度失败: {e}")
        # 返回降级的推荐资源
        try:
            return _generate_recommended_resources(disaster_type, location, level)
        except:
            return []


def check_resource_database_status() -> Dict[str, Any]:
    """检查资源数据库状态。"""
    conn = _get_db_connection()
    
    if not conn:
        return {"connected": False, "error": "无法连接数据库"}
    
    try:
        cursor = conn.cursor()
        
        # 检查表是否存在
        cursor.execute("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = %s AND table_name = 'emergency_resource');", (MYSQL_DATABASE,))
        table_exists = cursor.fetchone()[0]
        
        row_count = 0
        if table_exists:
            cursor.execute("SELECT COUNT(*) FROM emergency_resource;")
            row_count = cursor.fetchone()[0]
        
        cursor.close()
        
        return {
            "connected": True,
            "database": MYSQL_DATABASE,
            "table_exists": table_exists,
            "row_count": row_count
        }
    
    except Exception as e:
        return {"connected": False, "error": str(e)}
    finally:
        if conn:
            conn.close()


if __name__ == "__main__":
    # 测试资源调度功能
    test_info = {
        "type": "地震",
        "level": "高",
        "location": "云南省昆明市五华区",
        "affected_population": 500,
        "confidence": 0.9
    }
    
    print("检查数据库状态:")
    status = check_resource_database_status()
    print(json.dumps(status, ensure_ascii=False, indent=2))
    
    print("\n资源调度结果:")
    result = dispatch_resources(test_info)
    for i, resource in enumerate(result, 1):
        print(f"{i}. {resource['name']} ({resource['type']}): {resource['quantity']} {resource.get('unit', '')}, 优先级: {resource.get('priority', '中')}")
