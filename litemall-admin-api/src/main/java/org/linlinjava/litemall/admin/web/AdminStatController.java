package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.admin.vo.StatVo;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.service.StatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/stat")
@Validated
public class AdminStatController {
    private final Log logger = LogFactory.getLog(AdminStatController.class);

    private static final Set<String> SUPPORTED_PERIODS = new HashSet<>(Arrays.asList("day", "month", "quarter", "year"));

    @Autowired
    private StatService statService;

    @RequiresPermissions("admin:stat:user")
    @RequiresPermissionsDesc(menu = {"统计管理", "用户统计"}, button = "查询")
    @GetMapping("/user")
    public Object statUser() {
        List<Map> rows = statService.statUser();
        String[] columns = new String[]{"day", "users"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);
        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:order")
    @RequiresPermissionsDesc(menu = {"统计管理", "订单统计"}, button = "查询")
    @GetMapping("/order")
    public Object statOrder(String period, Integer categoryId) {
        String finalPeriod = (period == null || period.isEmpty()) ? "day" : period;
        if (!SUPPORTED_PERIODS.contains(finalPeriod)) {
            return ResponseUtil.badArgumentValue();
        }
        Integer finalCategoryId = categoryId == null ? 0 : categoryId;
        if (finalCategoryId < 0) {
            return ResponseUtil.badArgumentValue();
        }
        List<Map> rows = statService.statOrder(finalPeriod, finalCategoryId);
        String[] columns = new String[]{"day", "orders", "customers", "amount", "pcr"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);

        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:goods")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品统计"}, button = "查询")
    @GetMapping("/goods")
    public Object statGoods() {
        List<Map> rows = statService.statGoods();
        String[] columns = new String[]{"day", "orders", "products", "amount"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);
        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:comment")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品打分统计"}, button = "查询")
    @GetMapping("/comment")
    public Object statGoodsComment(Integer categoryId, String order) {
        Integer finalCategoryId = categoryId == null ? 0 : categoryId;
        if (finalCategoryId < 0) {
            return ResponseUtil.badArgumentValue();
        }

        String finalOrder = StringUtils.hasText(order) ? order.toLowerCase() : "desc";
        if (!"asc".equals(finalOrder) && !"desc".equals(finalOrder)) {
            return ResponseUtil.badArgumentValue();
        }

        List<Map> rows = statService.statGoodsComment(finalCategoryId, finalOrder);
        String[] columns = new String[]{"goodsId", "goodsName", "categoryName", "avgStar", "userCount"};
        StatVo statVo = new StatVo();
        statVo.setColumns(columns);
        statVo.setRows(rows);
        return ResponseUtil.ok(statVo);
    }

    @RequiresPermissions("admin:stat:commentwordcloud")
    @RequiresPermissionsDesc(menu = {"统计管理", "商品评论词云"}, button = "查询")
    @GetMapping("/comment/wordcloud")
    public Object statCommentWordcloud(Integer goodsId, Integer categoryId, Integer sample, Integer top) {
        Integer finalGoodsId = goodsId == null ? 0 : goodsId;
        Integer finalCategoryId = categoryId == null ? 0 : categoryId;
        if (finalGoodsId < 0 || finalCategoryId < 0) {
            return ResponseUtil.badArgumentValue();
        }
        int finalSample = sample == null ? 500 : sample;
        if (finalSample < 50) {
            finalSample = 50;
        }
        if (finalSample > 2000) {
            finalSample = 2000;
        }
        int finalTop = top == null ? 50 : top;
        if (finalTop < 10) {
            finalTop = 10;
        }
        if (finalTop > 200) {
            finalTop = 200;
        }
        List<Map<String, Object>> data = statService.statCommentWordcloud(finalGoodsId, finalCategoryId, finalSample, finalTop);
        return ResponseUtil.ok(data);
    }
}
