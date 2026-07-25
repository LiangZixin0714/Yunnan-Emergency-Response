USE emergency_db;
INSERT INTO emergency_resource (resource_id, resource_name, resource_type, total_stock, available_stock, locked_stock, unit, status, location, description, created_at, updated_at) VALUES
('RES-0021', '地震救援队E组', 'team', 35, 30, 5, '人', 'available', '昆明市', '专业地震救援队伍', NOW(), NOW()),
('RES-0022', '应急药品箱', 'medical', 100, 90, 10, '箱', 'available', '曲靖市', '急救药品套装', NOW(), NOW());