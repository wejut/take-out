package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.ShoppingCart;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface ShoppingCartMapper {
    /**
     * 动态查询购物车
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> search(ShoppingCart shoppingCart);

    /**
     * 对已存在购物车信息的数量加一根据id
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateCartNumber(ShoppingCart shoppingCart);

    /**
     * 插入新的购物车数据
     * @param shoppingCart
     */
    @Insert("INSERT INTO shopping_cart (name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time)" +
            "VALUES(#{name}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{image}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     *根据用户id删除全部购物车
     * @param userId
     */
    @Delete("delete from Shopping_cart where user_id= #{userId}")
    void deleteAllByUserId(Long userId);

    /**
     * 根据购物车id删除购物车
     * @param id
     */
    @Delete("delete from shopping_cart where id =#{id}")
    void deleteOneCartById(Long id);

    /**
     * 批量插入购物车数据
     * @param carts
     */
    void insertBatch(List<ShoppingCart> carts);
}
