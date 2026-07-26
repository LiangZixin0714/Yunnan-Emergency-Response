package com.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.config.AmapConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AmapGeocodeService {

    private static final Logger logger = LoggerFactory.getLogger(AmapGeocodeService.class);

    private final RestTemplate amapRestTemplate;
    private final AmapConfig amapConfig;
    private final ObjectMapper objectMapper;

    private static final Map<String, double[]> CITY_COORDINATES = new HashMap<>();
    static {
        CITY_COORDINATES.put("昆明市", new double[]{25.0406, 102.7123});
        CITY_COORDINATES.put("五华区", new double[]{25.0406, 102.7123});
        CITY_COORDINATES.put("盘龙区", new double[]{25.0453, 102.7269});
        CITY_COORDINATES.put("官渡区", new double[]{25.0054, 102.7853});
        CITY_COORDINATES.put("西山区", new double[]{25.0389, 102.6578});
        CITY_COORDINATES.put("呈贡区", new double[]{24.8795, 102.8193});
        CITY_COORDINATES.put("晋宁区", new double[]{24.6757, 102.7197});
        CITY_COORDINATES.put("东川区", new double[]{26.0795, 103.1660});
        CITY_COORDINATES.put("安宁市", new double[]{24.9166, 102.4835});

        CITY_COORDINATES.put("昭通市", new double[]{27.3333, 103.7176});
        CITY_COORDINATES.put("昭阳区", new double[]{27.3333, 103.7176});
        CITY_COORDINATES.put("永善县", new double[]{28.2167, 103.6125});
        CITY_COORDINATES.put("大关县", new double[]{27.7500, 103.9372});
        CITY_COORDINATES.put("彝良县", new double[]{27.6236, 104.0545});
        CITY_COORDINATES.put("威信县", new double[]{27.8586, 105.0227});
        CITY_COORDINATES.put("盐津县", new double[]{28.0730, 104.0230});

        CITY_COORDINATES.put("大理州", new double[]{25.5926, 100.2330});
        CITY_COORDINATES.put("大理市", new double[]{25.5926, 100.2330});
        CITY_COORDINATES.put("下关镇", new double[]{25.6065, 100.2676});
        CITY_COORDINATES.put("大理古城", new double[]{25.7637, 100.1570});
        CITY_COORDINATES.put("洱源县", new double[]{25.9560, 99.9850});
        CITY_COORDINATES.put("宾川县", new double[]{25.8236, 100.5913});
        CITY_COORDINATES.put("剑川县", new double[]{26.5347, 99.8899});
        CITY_COORDINATES.put("鹤庆县", new double[]{26.5575, 100.1745});

        CITY_COORDINATES.put("丽江市", new double[]{26.8760, 100.2330});
        CITY_COORDINATES.put("古城区", new double[]{26.8760, 100.2330});
        CITY_COORDINATES.put("玉龙县", new double[]{27.1554, 100.2275});
        CITY_COORDINATES.put("永胜县", new double[]{26.7115, 100.7595});
        CITY_COORDINATES.put("华坪县", new double[]{26.6309, 101.2525});
        CITY_COORDINATES.put("宁蒗县", new double[]{27.3330, 100.8200});

        CITY_COORDINATES.put("普洱市", new double[]{22.8252, 100.9660});
        CITY_COORDINATES.put("思茅区", new double[]{22.8252, 100.9660});
        CITY_COORDINATES.put("宁洱县", new double[]{22.8278, 101.0759});
        CITY_COORDINATES.put("墨江县", new double[]{23.4389, 101.6607});
        CITY_COORDINATES.put("景东县", new double[]{24.4211, 100.8397});
        CITY_COORDINATES.put("澜沧县", new double[]{22.5524, 100.1015});

        CITY_COORDINATES.put("西双版纳州", new double[]{22.0678, 100.7971});
        CITY_COORDINATES.put("景洪市", new double[]{22.0074, 100.7971});
        CITY_COORDINATES.put("勐海县", new double[]{21.9758, 100.4152});
        CITY_COORDINATES.put("勐腊县", new double[]{21.4754, 101.5650});

        CITY_COORDINATES.put("红河州", new double[]{23.3556, 103.3843});
        CITY_COORDINATES.put("蒙自市", new double[]{23.3556, 103.3843});
        CITY_COORDINATES.put("个旧市", new double[]{23.3640, 103.1454});
        CITY_COORDINATES.put("开远市", new double[]{23.7194, 103.3909});
        CITY_COORDINATES.put("弥勒市", new double[]{24.4294, 103.4320});
        CITY_COORDINATES.put("建水县", new double[]{23.6443, 102.8392});
        CITY_COORDINATES.put("石屏县", new double[]{23.7200, 102.4829});
        CITY_COORDINATES.put("元阳县", new double[]{23.1667, 102.6879});
        CITY_COORDINATES.put("红河县", new double[]{23.3704, 102.4354});

        CITY_COORDINATES.put("曲靖市", new double[]{25.4900, 103.7944});
        CITY_COORDINATES.put("麒麟区", new double[]{25.4900, 103.7944});
        CITY_COORDINATES.put("沾益区", new double[]{25.8500, 103.8140});
        CITY_COORDINATES.put("宣威市", new double[]{26.2890, 104.0930});
        CITY_COORDINATES.put("富源县", new double[]{25.6658, 104.2400});
        CITY_COORDINATES.put("会泽县", new double[]{26.4111, 103.2955});
        CITY_COORDINATES.put("陆良县", new double[]{25.0385, 103.6370});
        CITY_COORDINATES.put("师宗县", new double[]{24.8250, 103.9860});
        CITY_COORDINATES.put("罗平县", new double[]{24.8830, 104.3100});

        CITY_COORDINATES.put("保山市", new double[]{25.1120, 99.1551});
        CITY_COORDINATES.put("隆阳区", new double[]{25.1120, 99.1551});
        CITY_COORDINATES.put("腾冲市", new double[]{24.9645, 98.4863});
        CITY_COORDINATES.put("龙陵县", new double[]{24.5840, 98.3680});
        CITY_COORDINATES.put("昌宁县", new double[]{24.9120, 99.6040});

        CITY_COORDINATES.put("临沧市", new double[]{23.8760, 100.0899});
        CITY_COORDINATES.put("临翔区", new double[]{23.8760, 100.0899});
        CITY_COORDINATES.put("凤庆县", new double[]{24.5840, 99.8890});
        CITY_COORDINATES.put("云县", new double[]{24.4540, 100.1240});
        CITY_COORDINATES.put("沧源县", new double[]{23.1460, 99.2380});

        CITY_COORDINATES.put("楚雄州", new double[]{25.0424, 101.5455});
        CITY_COORDINATES.put("楚雄市", new double[]{25.0424, 101.5455});
        CITY_COORDINATES.put("双柏县", new double[]{24.6830, 101.6250});
        CITY_COORDINATES.put("牟定县", new double[]{25.3180, 101.5420});
        CITY_COORDINATES.put("南华县", new double[]{25.1980, 101.2890});
        CITY_COORDINATES.put("姚安县", new double[]{25.3920, 101.2340});
        CITY_COORDINATES.put("大姚县", new double[]{25.7340, 101.0350});
        CITY_COORDINATES.put("永仁县", new double[]{26.0630, 101.8140});
        CITY_COORDINATES.put("元谋县", new double[]{25.7300, 101.8580});
        CITY_COORDINATES.put("武定县", new double[]{25.5460, 102.3870});
        CITY_COORDINATES.put("禄丰县", new double[]{25.1430, 102.0330});

        CITY_COORDINATES.put("文山州", new double[]{23.3694, 104.2399});
        CITY_COORDINATES.put("文山市", new double[]{23.3694, 104.2399});
        CITY_COORDINATES.put("砚山县", new double[]{23.7250, 104.3150});
        CITY_COORDINATES.put("西畴县", new double[]{23.4170, 104.6860});
        CITY_COORDINATES.put("马关县", new double[]{22.9370, 104.3930});
        CITY_COORDINATES.put("丘北县", new double[]{24.0410, 104.1850});
        CITY_COORDINATES.put("广南县", new double[]{24.0510, 105.0540});
        CITY_COORDINATES.put("富宁县", new double[]{23.6290, 105.6340});

        CITY_COORDINATES.put("德宏州", new double[]{24.4367, 98.5854});
        CITY_COORDINATES.put("芒市", new double[]{24.4367, 98.5854});
        CITY_COORDINATES.put("瑞丽市", new double[]{24.0530, 97.8520});
        CITY_COORDINATES.put("梁河县", new double[]{24.3170, 98.2900});
        CITY_COORDINATES.put("盈江县", new double[]{24.7150, 97.9310});
        CITY_COORDINATES.put("陇川县", new double[]{24.3380, 97.7920});

        CITY_COORDINATES.put("怒江州", new double[]{25.8588, 98.8563});
        CITY_COORDINATES.put("泸水市", new double[]{25.8588, 98.8563});
        CITY_COORDINATES.put("福贡县", new double[]{26.0330, 98.8790});
        CITY_COORDINATES.put("贡山县", new double[]{27.6640, 98.6360});
        CITY_COORDINATES.put("兰坪县", new double[]{26.4130, 99.4200});

        CITY_COORDINATES.put("迪庆州", new double[]{27.8184, 99.7062});
        CITY_COORDINATES.put("香格里拉市", new double[]{27.8184, 99.7062});
        CITY_COORDINATES.put("德钦县", new double[]{28.4770, 98.9050});
        CITY_COORDINATES.put("维西县", new double[]{27.1430, 99.2740});

        CITY_COORDINATES.put("玉溪市", new double[]{24.3518, 102.5457});
        CITY_COORDINATES.put("红塔区", new double[]{24.3518, 102.5457});
        CITY_COORDINATES.put("江川区", new double[]{24.3440, 102.7320});
        CITY_COORDINATES.put("澄江市", new double[]{24.6710, 102.9310});
        CITY_COORDINATES.put("通海县", new double[]{24.0930, 102.7640});
        CITY_COORDINATES.put("华宁县", new double[]{24.2640, 102.9270});
        CITY_COORDINATES.put("易门县", new double[]{24.6730, 102.1670});
        CITY_COORDINATES.put("峨山县", new double[]{24.1850, 102.3840});
        CITY_COORDINATES.put("新平县", new double[]{23.8800, 101.9920});
        CITY_COORDINATES.put("元江县", new double[]{23.4090, 101.9920});

        CITY_COORDINATES.put("安宁市", new double[]{24.9166, 102.4835});
        CITY_COORDINATES.put("宣威市", new double[]{26.2890, 104.0930});
        CITY_COORDINATES.put("弥勒市", new double[]{24.4294, 103.4320});
        CITY_COORDINATES.put("腾冲市", new double[]{24.9645, 98.4863});
    }

    public AmapGeocodeService(@Qualifier("amapRestTemplate") RestTemplate amapRestTemplate,
                              AmapConfig amapConfig,
                              ObjectMapper objectMapper) {
        this.amapRestTemplate = amapRestTemplate;
        this.amapConfig = amapConfig;
        this.objectMapper = objectMapper;
    }

    public double[] geocode(String address) {
        if (address == null || address.trim().isEmpty()) {
            logger.warn("地址为空，跳过地理编码");
            return null;
        }

        double[] cached = lookupCityCoordinate(address);
        if (cached != null) {
            logger.info("命中城市坐标缓存: address={}, longitude={}, latitude={}", address, cached[1], cached[0]);
            return cached;
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(amapConfig.getGeocodeUrl())
                    .queryParam("address", address)
                    .queryParam("key", amapConfig.getApiKey())
                    .build()
                    .toUriString();

            logger.info("调用高德地理编码API: address={}", address);

            String response = amapRestTemplate.getForObject(url, String.class);
            if (response == null) {
                logger.warn("高德API返回为空，尝试模糊匹配: address={}", address);
                return fallbackGeocode(address);
            }

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();
            if (!"1".equals(status)) {
                String info = root.path("info").asText();
                logger.warn("高德地理编码失败: status={}, info={}, address={}, 尝试模糊匹配", status, info, address);
                return fallbackGeocode(address);
            }

            JsonNode geocodes = root.path("geocodes");
            if (!geocodes.isArray() || geocodes.isEmpty()) {
                logger.warn("高德地理编码无结果，尝试模糊匹配: address={}", address);
                return fallbackGeocode(address);
            }

            String location = geocodes.get(0).path("location").asText();
            if (location == null || !location.contains(",")) {
                logger.warn("高德返回location格式异常: location={}, address={}", location, address);
                return fallbackGeocode(address);
            }

            String[] parts = location.split(",");
            double longitude = Double.parseDouble(parts[0].trim());
            double latitude = Double.parseDouble(parts[1].trim());

            logger.info("地理编码成功: address={}, longitude={}, latitude={}", address, longitude, latitude);
            return new double[]{latitude, longitude};

        } catch (Exception e) {
            logger.error("高德地理编码异常，尝试模糊匹配: address={}", address, e);
            return fallbackGeocode(address);
        }
    }

    private double[] lookupCityCoordinate(String address) {
        if (address == null) return null;
        double[] bestMatch = null;
        int bestLength = 0;
        for (Map.Entry<String, double[]> entry : CITY_COORDINATES.entrySet()) {
            String key = entry.getKey();
            if (address.contains(key) && key.length() > bestLength) {
                bestMatch = entry.getValue().clone();
                bestLength = key.length();
            }
        }
        return bestMatch;
    }

    private double[] fallbackGeocode(String address) {
        double[] result = lookupCityCoordinate(address);
        if (result != null) {
            logger.info("模糊匹配成功: address={}, longitude={}, latitude={}", address, result[1], result[0]);
            return result;
        }
        logger.warn("地理编码完全失败: address={}", address);
        return null;
    }
}
