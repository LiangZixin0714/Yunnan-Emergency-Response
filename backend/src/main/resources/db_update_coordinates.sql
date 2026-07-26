-- 为历史灾情数据补充经纬度（按城市名称匹配）
-- 云南省主要城市坐标 (WGS84近似坐标)

UPDATE incidents SET latitude = 25.0406, longitude = 102.7123 WHERE location LIKE '%昆明%' AND latitude IS NULL;
UPDATE incidents SET latitude = 27.3333, longitude = 103.7176 WHERE location LIKE '%昭通%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.5926, longitude = 100.2330 WHERE location LIKE '%大理%' AND latitude IS NULL;
UPDATE incidents SET latitude = 22.8252, longitude = 100.9660 WHERE location LIKE '%普洱%' AND latitude IS NULL;
UPDATE incidents SET latitude = 26.8760, longitude = 100.2330 WHERE location LIKE '%丽江%' AND latitude IS NULL;
UPDATE incidents SET latitude = 22.0678, longitude = 100.7971 WHERE location LIKE '%西双版纳%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.3556, longitude = 103.3843 WHERE location LIKE '%红河%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.4900, longitude = 103.7944 WHERE location LIKE '%曲靖%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.1120, longitude = 99.1551 WHERE location LIKE '%保山%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.8760, longitude = 100.0899 WHERE location LIKE '%临沧%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.0424, longitude = 101.5455 WHERE location LIKE '%楚雄%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.3694, longitude = 104.2399 WHERE location LIKE '%文山%' AND latitude IS NULL;
UPDATE incidents SET latitude = 24.4367, longitude = 98.5854 WHERE location LIKE '%德宏%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.8588, longitude = 98.8563 WHERE location LIKE '%怒江%' AND latitude IS NULL;
UPDATE incidents SET latitude = 27.8184, longitude = 99.7062 WHERE location LIKE '%迪庆%' AND latitude IS NULL;
UPDATE incidents SET latitude = 24.3518, longitude = 102.5457 WHERE location LIKE '%玉溪%' AND latitude IS NULL;

-- 兜底：按incident_id批量更新（如果location匹配不到）
UPDATE incidents SET latitude = 25.0406, longitude = 102.7123 WHERE incident_id LIKE '%0001%' AND latitude IS NULL;
UPDATE incidents SET latitude = 27.3333, longitude = 103.7176 WHERE incident_id LIKE '%0002%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.5926, longitude = 100.2330 WHERE incident_id LIKE '%0003%' AND latitude IS NULL;
UPDATE incidents SET latitude = 22.8252, longitude = 100.9660 WHERE incident_id LIKE '%0004%' AND latitude IS NULL;
UPDATE incidents SET latitude = 26.8760, longitude = 100.2330 WHERE incident_id LIKE '%0005%' AND latitude IS NULL;
UPDATE incidents SET latitude = 22.0678, longitude = 100.7971 WHERE incident_id LIKE '%0006%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.3556, longitude = 103.3843 WHERE incident_id LIKE '%0007%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.4900, longitude = 103.7944 WHERE incident_id LIKE '%0008%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.1120, longitude = 99.1551 WHERE incident_id LIKE '%0009%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.8760, longitude = 100.0899 WHERE incident_id LIKE '%0010%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.0424, longitude = 101.5455 WHERE incident_id LIKE '%0011%' AND latitude IS NULL;
UPDATE incidents SET latitude = 23.3694, longitude = 104.2399 WHERE incident_id LIKE '%0012%' AND latitude IS NULL;
UPDATE incidents SET latitude = 24.4367, longitude = 98.5854 WHERE incident_id LIKE '%0013%' AND latitude IS NULL;
UPDATE incidents SET latitude = 25.8588, longitude = 98.8563 WHERE incident_id LIKE '%0014%' AND latitude IS NULL;
UPDATE incidents SET latitude = 27.8184, longitude = 99.7062 WHERE incident_id LIKE '%0015%' AND latitude IS NULL;
UPDATE incidents SET latitude = 24.3518, longitude = 102.5457 WHERE incident_id LIKE '%0016%' AND latitude IS NULL;

-- 统计结果
SELECT 
  COUNT(*) AS total_count,
  COUNT(latitude) AS has_coordinates,
  COUNT(*) - COUNT(latitude) AS missing_coordinates
FROM incidents;
