package com.oa2.util;

import com.oa2.dao.SignDao;
import com.oa2.pojo.Sign;
import com.oa2.repository.SignElasticsearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据迁移工具类
 * 用于将 MySQL 中的签到记录迁移到 Elasticsearch
 */
@Component
public class DataMigrationUtil {

    @Autowired(required = false)
    private SignDao signDao;

    @Autowired(required = false)
    private SignElasticsearchRepository signRepository;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchTemplate;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public int rebuildSignIndexFromMysql() {
        if (signDao == null || signRepository == null || elasticsearchTemplate == null) {
            throw new IllegalStateException("SignDao、SignElasticsearchRepository 或 ElasticsearchOperations 未正确注入");
        }

        IndexOperations indexOps = elasticsearchTemplate.indexOps(Sign.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping());

        List<Sign> mysqlSigns = signDao.selectAll();
        List<Sign> esSigns = new ArrayList<>();
        for (Sign mysqlSign : mysqlSigns) {
            esSigns.add(convertMysqlSignToElasticsearchSign(mysqlSign));
        }
        signRepository.saveAll(esSigns);
        return esSigns.size();
    }

    /**
     * 将 MySQL 中的签到数据迁移到 Elasticsearch
     * 注意：此方法需要临时将 MySQL 的 SignDao 启用
     */
    public void migrateSignDataFromMysqlToElasticsearch() {
        System.out.println("******将 MySQL 中的签到数据迁移到 Elasticsearch*****");
        if (signDao == null || signRepository == null) {
            System.err.println("数据迁移失败：SignDao 或 SignElasticsearchRepository 未正确注入");
            return;
        }

        try {
            System.out.println("开始数据迁移...");

            // 清空 Elasticsearch 中的现有数据（可选）
            signRepository.deleteAll();
            System.out.println("已清空 Elasticsearch 中的现有数据");

            List<Sign> mysqlSigns = signDao.selectAll();
            List<Sign> esSigns = new ArrayList<>();
            for (Sign mysqlSign : mysqlSigns) {
                esSigns.add(convertMysqlSignToElasticsearchSign(mysqlSign));
            }
            signRepository.saveAll(esSigns);
            System.out.println("迁移完成，共写入 Elasticsearch " + esSigns.size() + " 条记录");

        } catch (Exception e) {
            System.err.println("数据迁移过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将 MySQL 的 Sign 对象转换为 Elasticsearch 的 Sign 对象
     */
    private Sign convertMysqlSignToElasticsearchSign(Sign mysqlSign) {
        System.out.println("*****MySQL 的 Sign 对象转换为 Elasticsearch 的 Sign 对象****");
        Sign esSign = new Sign();
        
        esSign.setId(mysqlSign.getId());
        
        // 复制基本字段
        esSign.setSignDate(mysqlSign.getSignDate());
        esSign.setNumber(mysqlSign.getNumber());
        esSign.setState(mysqlSign.getState());
        esSign.setDept_name(mysqlSign.getDept_name());
        esSign.setName(mysqlSign.getName());
        esSign.setType(mysqlSign.getType());
        esSign.setSign_address(mysqlSign.getSign_address());
        esSign.setTag(mysqlSign.getTag());
        
        // 设置新增字段
        try {
            Date signDate = DU.parseDate(mysqlSign.getSignDate());
            esSign.setTimestamp(signDate.getTime());
            esSign.setDateOnly(dateFormat.format(signDate));
        } catch (Exception e) {
            // 如果日期解析失败，使用当前时间
            esSign.setTimestamp(System.currentTimeMillis());
            if (mysqlSign.getSignDate() != null && mysqlSign.getSignDate().length() >= 10) {
                esSign.setDateOnly(mysqlSign.getSignDate().substring(0, 10));
            } else {
                esSign.setDateOnly(dateFormat.format(new Date()));
            }
        }
        
        return esSign;
    }

    /**
     * 验证迁移结果
     */
    public void validateMigration() {
        if (signRepository == null) {
            System.err.println("验证失败：SignElasticsearchRepository 未正确注入");
            return;
        }

        try {
            long count = signRepository.count();
            System.out.println("Elasticsearch 中共有 " + count + " 条签到记录");

            // 可以添加更多验证逻辑
            List<Sign> recentSigns = signRepository.findAllByOrderByTimestampDesc();
            if (!recentSigns.isEmpty()) {
                System.out.println("最新的签到记录：");
                for (int i = 0; i < Math.min(5, recentSigns.size()); i++) {
                    Sign sign = recentSigns.get(i);
                    System.out.println("  员工编号: " + sign.getNumber() + 
                                     ", 签到时间: " + sign.getSignDate() + 
                                     ", 状态: " + sign.getState());
                }
            }
        } catch (Exception e) {
            System.err.println("验证过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
} 
