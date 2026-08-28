package com.sky.controller.user;


import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api("c端购物车端口")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;


    @PostMapping("/add")
    @ApiOperation(value="添加购物车")
//    public Result add(@RequestBody String body) {
//        System.out.println("收到数据：" + body);
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.add(shoppingCartDTO);
        return  Result.success();}

    @GetMapping("/list")
    @ApiOperation(value="查看购物车")
    public Result<List<ShoppingCart>> getAllCarts(){
        List<ShoppingCart> allCarts = shoppingCartService.getAllCarts();
        return Result.success(allCarts);
    }

    @DeleteMapping("/clean")
        @ApiOperation(value = "清空购物车")
        public Result deleteAll(){
            shoppingCartService.deleteAll();
            return Result.success();
    }

    @PostMapping("/sub")
    @ApiOperation(value="删除购物车里面一个商品")
    public Result deletePreciseOne(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.deletePreciseOne(shoppingCartDTO);
        return Result.success();
    }
}
