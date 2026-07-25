INSERT INTO emergency_resource (resource_id, resource_name, resource_type, unit, total_stock, available_stock, locked_stock, location, status, description)
VALUES
('RES-DIGGER-001', '大型挖掘机', '设备', '台', 20, 15, 5, '云南省应急物资储备库', 'available', '大型履带式挖掘机，用于泥石流清障、道路疏通'),
('RES-TENT-001', '救灾应急帐篷', '物资', '顶', 2000, 1500, 500, '昆明市物资仓库', 'available', '10人救灾帐篷，防风防雨，快速搭建'),
('RES-MASK-001', 'N95医疗口罩', '医疗', '只', 50000, 45000, 5000, '云南省疾控中心', 'available', 'N95级防护口罩，一次性使用'),
('RES-GEN-001', '应急发电机', '设备', '台', 30, 25, 5, '昆明市供电局', 'available', '5kw柴油发电机，应急供电'),
('RES-WATER-001', '瓶装矿泉水', '生活', '箱', 10000, 8000, 2000, '昆明市储备库', 'available', '550ml瓶装矿泉水，每箱24瓶');