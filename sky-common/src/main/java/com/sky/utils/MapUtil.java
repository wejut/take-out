package com.sky.utils;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sky.properties.BaiDuMapProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;


@Component
public class MapUtil {
    //两个接口，做两个方法
    @Autowired
    private BaiDuMapProperties baiDuMapProperties;
    //方法一，把address丢进去，用map封装经纬度
    public HashMap getLatAndLong(String address){
        String ak = baiDuMapProperties.getAk();
        String type = "json";
        HashMap<String, String> map = new HashMap<>();
        map.put("address",address);
        map.put("output",type);
        map.put("ak",ak);
        String response = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3/", map);
        JSONObject object = JSONUtil.parseObj(response);
        String status = object.getStr("status");
        JSONObject location = object.getJSONObject("result").getJSONObject("location");
        if(!status.equals("0")){
//            throw new OrderBusinessException("收货地址解析失败");
            //返回错误
            return null;
        }
        float lng = location.getFloat("lng");
        Float lat = location.getFloat("lat");
        HashMap<String, Float> map1= new HashMap<>();
        map1.put("lng",lng);
        map1.put("lat",lat);
        return map1;
    }
    //方法二，传两个地址进来，返回boolean
    public HashMap isTooFar(double lng, double lat){
        HashMap shopLatAndLong = this.getLatAndLong(baiDuMapProperties.getAddress());
        if (shopLatAndLong == null || shopLatAndLong.isEmpty()) {
            return null; // 异常
        }
        String origin = shopLatAndLong.get("lat")+","+shopLatAndLong.get("lng");
        String destination = lat+","+lng;
        String ak = baiDuMapProperties.getAk();
        HashMap<String, String> map2 = new HashMap<>();
        map2.put("ak",ak);
        map2.put("origin",origin);
        map2.put("destination",destination);
        map2.put("steps_info","0");
        String response = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/riding", map2);
        JSONObject object = JSONUtil.parseObj(response);
        if(!object.getStr("status").equals("0")){
//            throw new OrderBusinessException;
            return null;
        }
        HashMap<String,Boolean> map3 = new HashMap<>();
        JSONObject result = object.getJSONObject("result");
        JSONArray routes =  result.getJSONArray("routes");
        Integer distance = (Integer)(((JSONObject) routes.get(0)).get("distance"));
        if (distance>5000){
            map3.put("0F1T",false);
            return map3;
        }
        map3.put("0F1T",true);
        return map3;
    }
}
