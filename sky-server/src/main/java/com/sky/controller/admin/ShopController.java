package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


@RestController("adminShopController")
@Slf4j
@RequestMapping("/admin/shop")
@Api(tags = "店铺接口")
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation(value = "店铺状态修改")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态为：{}",status==1?"营业中":"休业中");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }


    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取到的店铺状态为：{}",shopStatus==1?"营业中":"休业中");
        return Result.success(shopStatus);
    }
}
