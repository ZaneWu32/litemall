package org.linlinjava.litemall.db.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface StatMapper {
    List<Map> statUser();

    List<Map> statOrder(@Param("period") String period, @Param("categoryId") Integer categoryId);

    List<Map> statGoods();

    List<Map> statGoodsComment(@Param("categoryId") Integer categoryId, @Param("order") String order);

    List<String> listCommentContents(@Param("goodsId") Integer goodsId,
                                     @Param("categoryId") Integer categoryId,
                                     @Param("limit") Integer limit);
}
