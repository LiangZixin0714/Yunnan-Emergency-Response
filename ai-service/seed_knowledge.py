"""向 knowledge_chunks 表插入示例数据的种子脚本。"""

import os
import sys
import json

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import psycopg2

PG_HOST = os.environ.get("PG_HOST", "localhost")
PG_PORT = int(os.environ.get("PG_PORT", "5432"))
PG_DATABASE = os.environ.get("PG_DATABASE", "emergency_vector")
PG_USER = os.environ.get("PG_USER", "postgres")
PG_PASSWORD = os.environ.get("PG_PASSWORD", "ZAQ12wsx581!")

SAMPLE_CHUNKS = [
    {
        "chunk_id": "earthquake_001",
        "document_name": "云南省地震应急预案",
        "document_type": "综合应急预案",
        "chapter": "地震应急响应",
        "section": "地震响应分级",
        "page": 5,
        "content": "根据地震灾害的影响范围和严重程度，地震应急响应分为四级：一级响应（特别重大地震）、二级响应（重大地震）、三级响应（较大地震）和四级响应（一般地震）。一级响应由省政府启动，需调动全省资源进行救援。二级响应由省应急管理厅启动，重点调动省级专业救援力量。三级和四级响应由州市县级启动，以属地救援为主。",
        "source": "云南省应急管理厅",
        "publish_org": "云南省应急管理厅",
        "publish_date": "2024-03-01",
        "version": "v1.0",
        "order": 1,
    },
    {
        "chunk_id": "earthquake_002",
        "document_name": "云南省地震应急预案",
        "document_type": "综合应急预案",
        "chapter": "地震应急响应",
        "section": "救援力量调度",
        "page": 8,
        "content": "地震发生后，应第一时间调度专业救援力量赶赴现场。主要包括：省消防救援总队的地震救援队、省地震局的专业监测队伍、省卫健委的医疗救援队伍、省军区的预备役部队和民兵力量。同时，根据灾情需要，可向国家应急管理部申请调派国家综合性消防救援队伍增援。救援力量应携带生命探测仪、破拆装备、医疗急救设备等专业装备。",
        "source": "云南省应急管理厅",
        "publish_org": "云南省应急管理厅",
        "publish_date": "2024-03-01",
        "version": "v1.0",
        "order": 2,
    },
    {
        "chunk_id": "earthquake_003",
        "document_name": "云南省地震应急预案",
        "document_type": "综合应急预案",
        "chapter": "地震应急响应",
        "section": "伤员救治与转运",
        "page": 12,
        "content": "地震造成人员伤亡时，应立即启动医疗救援响应。各医疗机构应预留急诊床位，组建应急医疗救治队伍。现场救援人员应优先救治重伤员，利用急救包、担架等设备进行现场急救。重伤员应通过救护车、直升机等方式尽快转运至有条件的医疗机构。同时，要做好伤员的心理疏导工作，组织心理医生开展心理干预。",
        "source": "云南省卫健委",
        "publish_org": "云南省卫健委",
        "publish_date": "2024-03-01",
        "version": "v1.0",
        "order": 3,
    },
    {
        "chunk_id": "earthquake_004",
        "document_name": "云南省地震应急预案",
        "document_type": "综合应急预案",
        "chapter": "地震应急响应",
        "section": "次生灾害防范",
        "page": 15,
        "content": "地震可能引发火灾、爆炸、有毒有害气体泄漏、泥石流、山体滑坡、堰塞湖等次生灾害。应加强对危化品企业、水库大坝、尾矿库、输油输气管道等重点设施的监测监控。消防队伍应及时赶赴火灾现场扑救。自然资源部门应组织监测队伍对滑坡、泥石流隐患点进行巡查。水利部门应加强对水库大坝的安全检查，必要时启动泄洪措施。",
        "source": "云南省应急管理厅",
        "publish_org": "云南省应急管理厅",
        "publish_date": "2024-03-01",
        "version": "v1.0",
        "order": 4,
    },
    {
        "chunk_id": "flood_001",
        "document_name": "云南省洪涝泥石流灾害卫生应急预案",
        "document_type": "专项应急预案",
        "chapter": "洪涝灾害应急响应",
        "section": "防汛响应行动",
        "page": 3,
        "content": "当江河水位接近警戒水位或气象部门预报有强降雨时，应立即启动防汛应急响应。各级防汛指挥部应实行24小时值班值守，加强对堤防、水库、水闸等水利工程的巡查。发现险情后，应立即组织抢险队伍进行加固处理。同时，做好低洼地区群众的转移安置工作，确保人民群众生命安全。",
        "source": "云南省水利厅",
        "publish_org": "云南省水利厅",
        "publish_date": "2024-05-01",
        "version": "v1.0",
        "order": 5,
    },
    {
        "chunk_id": "flood_002",
        "document_name": "云南省洪涝泥石流灾害卫生应急预案",
        "document_type": "专项应急预案",
        "chapter": "洪涝灾害应急响应",
        "section": "泥石流防范",
        "page": 7,
        "content": "泥石流灾害具有突发性强、破坏力大的特点。在汛期或强降雨期间，应加强对山区、丘陵地区的监测预警。国土资源部门应在泥石流易发区设置监测点，安装预警设备。一旦发现险情，应立即通过广播、短信、村村通等方式通知受影响群众撤离。撤离路线应选择坚实、平缓的地段，避开沟谷、陡坡等危险区域。",
        "source": "云南省自然资源厅",
        "publish_org": "云南省自然资源厅",
        "publish_date": "2024-05-01",
        "version": "v1.0",
        "order": 6,
    },
    {
        "chunk_id": "fire_001",
        "document_name": "云南省森林草原火灾应急预案",
        "document_type": "专项应急预案",
        "chapter": "森林草原火灾应急响应",
        "section": "火情监测与报告",
        "page": 4,
        "content": "森林草原火灾的监测主要依靠卫星遥感、航空巡护、瞭望塔监测和地面巡查四种方式。一旦发现火情，应立即核实起火地点、起火原因、火势大小等信息，并按规定逐级上报。火情报告应包括：起火时间、起火地点、火场面积、火势蔓延方向、风向风速、周边植被类型等关键信息。",
        "source": "云南省林业和草原局",
        "publish_org": "云南省林业和草原局",
        "publish_date": "2024-02-01",
        "version": "v1.0",
        "order": 7,
    },
    {
        "chunk_id": "fire_002",
        "document_name": "云南省森林草原火灾应急预案",
        "document_type": "专项应急预案",
        "chapter": "森林草原火灾应急响应",
        "section": "扑火力量组织",
        "page": 9,
        "content": "扑火力量分为专业扑火队伍和半专业扑火队伍两类。专业队伍包括：省森林消防总队、各州地市专业扑火队，配备风力灭火机、水枪、油锯等专业装备。半专业队伍以县市、乡镇为单位组建，作为专业力量的补充。扑火时应遵循＂先控制、后消灭＂的原则，优先灭火头，防止火势扩大。同时要设置安全观察员，确保扑火人员安全。",
        "source": "云南省林业和草原局",
        "publish_org": "云南省林业和草原局",
        "publish_date": "2024-02-01",
        "version": "v1.0",
        "order": 8,
    },
    {
        "chunk_id": "landslide_001",
        "document_name": "云南省突发地质灾害应急预案",
        "document_type": "专项应急预案",
        "chapter": "地质灾害应急响应",
        "section": "滑坡监测预警",
        "page": 6,
        "content": "滑坡监测预警系统包括专业监测和群测群防两个体系。专业监测由国土资源部门委托专业机构，在重点滑坡隐患点安装位移监测、雨量监测、地下水监测等设备。群测群防由基层干部群众组成监测小组，定期对责任区进行巡查。当监测数据超过预警阈值或发现异常变化时，应立即发布预警信息，组织群众撤离。",
        "source": "云南省自然资源厅",
        "publish_org": "云南省自然资源厅",
        "publish_date": "2024-04-01",
        "version": "v1.0",
        "order": 9,
    },
    {
        "chunk_id": "meteorology_001",
        "document_name": "云南省气象灾害应急预案",
        "document_type": "专项应急预案",
        "chapter": "气象灾害应急响应",
        "section": "气象预警发布",
        "page": 2,
        "content": "气象灾害预警信号分为暴雨、台风、高温、寒潮、雷电、冰雹、霜冻、大雾、霾等类型，按颜色分为蓝色、黄色、橙色、红色四级。预警信息发布后，各相关部门应根据预警等级，启动相应的应急响应措施。广播、电视、互联网等媒体应及时传播预警信息，提醒社会公众做好防范准备。",
        "source": "云南省气象局",
        "publish_org": "云南省气象局",
        "publish_date": "2024-06-01",
        "version": "v1.0",
        "order": 10,
    },
    {
        "chunk_id": "drought_001",
        "document_name": "云南省防汛抗旱应急预案",
        "document_type": "专项应急预案",
        "chapter": "抗旱应急响应",
        "section": "抗旱措施",
        "page": 8,
        "content": "干旱灾害应急响应分为四级：Ⅰ级（特别重大干旱）、Ⅱ级（重大干旱）、Ⅲ级（较大干旱）、Ⅳ级（一般干旱）。抗旱措施包括：合理调度水资源、实施人工增雨、组织抗旱服务队、启用应急灌溉设施、限时供水、应急调水等。对于饮水困难地区，应组织送水车为群众提供基本生活用水，确保人畜饮水安全。",
        "source": "云南省水利厅",
        "publish_org": "云南省水利厅",
        "publish_date": "2024-03-15",
        "version": "v1.0",
        "order": 11,
    },
    {
        "chunk_id": "rescue_001",
        "document_name": "云南省自然灾害救助应急预案",
        "document_type": "综合应急预案",
        "chapter": "灾害救助",
        "section": "受灾群众安置",
        "page": 10,
        "content": "自然灾害发生后，各级民政部门应立即组织受灾群众的基本生活救助。安置方式包括：就地安置、异地安置和投亲靠友三种。安置点应提供帐篷、食品、饮水、棉被等基本生活物资，并设置医疗点、心理咨询点、厕所等配套设施。同时，要做好安置点的卫生防疫工作，防止传染病的发生和传播。",
        "source": "云南省民政厅",
        "publish_org": "云南省民政厅",
        "publish_date": "2024-01-01",
        "version": "v1.0",
        "order": 12,
    },
]


def seed():
    conn = psycopg2.connect(
        host=PG_HOST, port=PG_PORT, database=PG_DATABASE,
        user=PG_USER, password=PG_PASSWORD
    )
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS knowledge_chunks (
            id SERIAL PRIMARY KEY,
            chunk_id VARCHAR(255) UNIQUE NOT NULL,
            document_name VARCHAR(255) NOT NULL,
            document_type VARCHAR(50) NOT NULL,
            chapter VARCHAR(50) NOT NULL,
            section VARCHAR(50),
            page INTEGER NOT NULL,
            content TEXT NOT NULL,
            length INTEGER NOT NULL,
            "order" INTEGER NOT NULL,
            source VARCHAR(255) NOT NULL,
            publish_org VARCHAR(255) NOT NULL,
            publish_date VARCHAR(20),
            version VARCHAR(50) NOT NULL,
            embedding double precision[],
            model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    """)
    conn.commit()
    print("✅ 表结构检查完成")

    cur.execute("SELECT COUNT(*) FROM knowledge_chunks;")
    existing = cur.fetchone()[0]
    print(f"📊 当前已有 {existing} 条数据")

    if existing > 0:
        print("⚠️  表中已有数据，跳过种子数据插入")
        cur.close()
        conn.close()
        return

    from rag.retriever import generate_embedding

    inserted = 0
    for chunk in SAMPLE_CHUNKS:
        try:
            embedding = generate_embedding(chunk["content"])
            if embedding is None:
                print(f"❌ 生成向量失败: {chunk['chunk_id']}")
                continue

            cur.execute("""
                INSERT INTO knowledge_chunks
                    (chunk_id, document_name, document_type, chapter, section,
                     page, content, length, "order", source, publish_org,
                     publish_date, version, embedding)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (chunk_id) DO NOTHING;
            """, (
                chunk["chunk_id"],
                chunk["document_name"],
                chunk["document_type"],
                chunk["chapter"],
                chunk["section"],
                chunk["page"],
                chunk["content"],
                len(chunk["content"]),
                chunk["order"],
                chunk["source"],
                chunk["publish_org"],
                chunk["publish_date"],
                chunk["version"],
                embedding,
            ))
            inserted += 1
            print(f"✅ 插入: {chunk['chunk_id']} - {chunk['document_name']}")
        except Exception as e:
            print(f"❌ 插入失败 {chunk['chunk_id']}: {e}")
            conn.rollback()

    conn.commit()
    cur.close()
    conn.close()
    print(f"\n🎉 种子数据插入完成，共插入 {inserted} 条数据")


if __name__ == "__main__":
    seed()
