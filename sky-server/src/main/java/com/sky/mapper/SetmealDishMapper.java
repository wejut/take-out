package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id获得关联套餐ids 用于查询菜品是否有关联套餐（于菜品删除功能）
     * @param ids
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> ids);


    /**
     * 批量插入菜品
     * @param dishes
     */
    void insertBatch(List<SetmealDish> dishes);

    /**
     * 根据套餐id批量删除套餐菜品表中的菜品关联套餐数据
     * @param id
     */
    void deleteMealDishByMealId(Long id);

    /**
     * 根据套餐id查询菜品与套餐的关联信息
     * @param id
     * @return
     */
    List<SetmealDish> getsetmealDishById(Long id);
}
