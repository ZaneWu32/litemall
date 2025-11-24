package org.linlinjava.litemall.db.service;

import org.linlinjava.litemall.db.dao.StatMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StatService {
    @Resource
    private StatMapper statMapper;


    public List<Map> statUser() {
        return statMapper.statUser();
    }

    public List<Map> statOrder(String period, Integer categoryId) {
        return statMapper.statOrder(period, categoryId);
    }

    public List<Map> statGoods() {
        return statMapper.statGoods();
    }

    public List<Map> statGoodsComment(Integer categoryId, String order) {
        return statMapper.statGoodsComment(categoryId, order);
    }

    public List<Map<String, Object>> statCommentWordcloud(Integer goodsId, Integer categoryId, Integer sample, Integer top) {
        int fetch = sample == null || sample <= 0 ? 500 : sample;
        List<String> contents = statMapper.listCommentContents(goodsId, categoryId, fetch);
        if (contents == null) {
            contents = new ArrayList<>();
        }
        Map<String, Integer> frequency = new HashMap<>();
        for (String content : contents) {
            if (content == null || content.isEmpty()) {
                continue;
            }
            tokenize(content, frequency);
        }
        int finalTop = top == null || top <= 0 ? 50 : top;
        return frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(finalTop)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>(2);
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
    private static final Pattern ALNUM_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");

    private void tokenize(String raw, Map<String, Integer> frequency) {
        String normalized = raw.replace('\n', ' ').replace('\r', ' ');
        Matcher cnMatcher = CHINESE_PATTERN.matcher(normalized);
        while (cnMatcher.find()) {
            accumulateToken(cnMatcher.group(), frequency);
        }
        Matcher enMatcher = ALNUM_PATTERN.matcher(normalized.toLowerCase());
        while (enMatcher.find()) {
            accumulateToken(enMatcher.group(), frequency);
        }
    }

    private void accumulateToken(String token, Map<String, Integer> frequency) {
        if (token == null) {
            return;
        }
        String trimmed = token.trim();
        if (trimmed.length() < 2) {
            return;
        }
        boolean allDigit = trimmed.chars().allMatch(Character::isDigit);
        if (allDigit) {
            return;
        }
        frequency.merge(trimmed, 1, Integer::sum);
    }
}
