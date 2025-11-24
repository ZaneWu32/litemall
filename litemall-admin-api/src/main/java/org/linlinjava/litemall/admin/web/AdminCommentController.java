package org.linlinjava.litemall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallCategory;
import org.linlinjava.litemall.db.domain.LitemallComment;
import org.linlinjava.litemall.db.domain.LitemallGoods;
import org.linlinjava.litemall.db.service.LitemallCategoryService;
import org.linlinjava.litemall.db.service.LitemallCommentService;
import org.linlinjava.litemall.db.service.LitemallGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/comment")
@Validated
public class AdminCommentController {
    private final Log logger = LogFactory.getLog(AdminCommentController.class);

    @Autowired
    private LitemallCommentService commentService;
    @Autowired
    private LitemallGoodsService goodsService;
    @Autowired
    private LitemallCategoryService categoryService;

    @RequiresPermissions("admin:comment:list")
    @RequiresPermissionsDesc(menu = {"商品管理", "评论管理"}, button = "查询")
    @GetMapping("/list")
    public Object list(String userId, String valueId, String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer limit,
                       @Sort @RequestParam(defaultValue = "add_time") String sort,
                       @Order @RequestParam(defaultValue = "desc") String order) {
        String resolvedUserId = userId;
        String goodsName = null;
        if (!StringUtils.isEmpty(keyword)) {
            String trimmed = keyword.trim();
            if (trimmed.matches("^\\d+$")) {
                resolvedUserId = trimmed;
            } else {
                goodsName = trimmed;
            }
        }
        List<LitemallComment> commentList = commentService.querySelective(resolvedUserId, valueId, goodsName, page, limit, sort, order);
        enrichCommentGoodsInfo(commentList);
        return ResponseUtil.okList(commentList);
    }

    @RequiresPermissions("admin:comment:delete")
    @RequiresPermissionsDesc(menu = {"商品管理", "评论管理"}, button = "删除")
    @PostMapping("/delete")
    public Object delete(@RequestBody LitemallComment comment) {
        Integer id = comment.getId();
        if (id == null) {
            return ResponseUtil.badArgument();
        }
        commentService.deleteById(id);
        return ResponseUtil.ok();
    }

    private void enrichCommentGoodsInfo(List<LitemallComment> commentList) {
        if (commentList == null || commentList.isEmpty()) {
            return;
        }
        List<Integer> goodsIds = commentList.stream()
                .map(LitemallComment::getValueId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (goodsIds.isEmpty()) {
            return;
        }
        List<LitemallGoods> goods = goodsService.queryByIds(goodsIds);
        Map<Integer, LitemallGoods> goodsMap = goods.stream()
                .collect(Collectors.toMap(LitemallGoods::getId, Function.identity()));
        List<Integer> categoryIds = goods.stream()
                .map(LitemallGoods::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<LitemallCategory> categories = categoryIds.isEmpty() ? new ArrayList<>() : categoryService.queryByIds(categoryIds);
        Map<Integer, LitemallCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(LitemallCategory::getId, Function.identity()));
        for (LitemallComment comment : commentList) {
            LitemallGoods goodsInfo = goodsMap.get(comment.getValueId());
            if (goodsInfo != null) {
                comment.setGoodsName(goodsInfo.getName());
                LitemallCategory category = categoryMap.get(goodsInfo.getCategoryId());
                if (category != null) {
                    comment.setCategoryName(category.getName());
                }
            }
        }
    }

}
