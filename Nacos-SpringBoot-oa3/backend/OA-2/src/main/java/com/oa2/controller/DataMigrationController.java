package com.oa2.controller;

import com.oa2.util.DataMigrationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.web.bind.annotation.*;
import com.oa2.pojo.Sign;

/**
 * 数据迁移控制器
 * 提供数据迁移相关的 API 接口
 */
@RestController
@RequestMapping("/migration")
@CrossOrigin
public class DataMigrationController {

    @Autowired
    private DataMigrationUtil dataMigrationUtil;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchTemplate;

    /**
     * 从 MySQL 全量重建 Elasticsearch 索引
     */
    @PostMapping("/rebuild-index")
    public RESP rebuildIndex() {
        try {
            int count = dataMigrationUtil.rebuildSignIndexFromMysql();
            return RESP.ok("索引重建成功，已从 MySQL 同步 " + count + " 条签到记录");
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("索引重建失败：" + e.getMessage());
        }
    }

    /**
     * 验证 Elasticsearch 迁移结果
     */
    @GetMapping("/validate")
    public RESP validateMigration() {
        try {
            dataMigrationUtil.validateMigration();
            return RESP.ok("验证完成，请查看控制台日志");
        } catch (Exception e) {
            return RESP.error("验证失败：" + e.getMessage());
        }
    }

    /**
     * 获取迁移状态信息
     */
    @GetMapping("/status")
    public RESP getMigrationStatus() {
        try {
            if (elasticsearchTemplate != null) {
                IndexOperations indexOps = elasticsearchTemplate.indexOps(Sign.class);
                boolean indexExists = indexOps.exists();
                
                String status = "当前使用 Elasticsearch 存储签到记录，索引状态：" + 
                               (indexExists ? "已创建" : "未创建");
                
                return RESP.ok(status);
            } else {
                return RESP.ok("当前使用 MySQL 存储签到记录");
            }
        } catch (Exception e) {
            return RESP.error("获取状态失败：" + e.getMessage());
        }
    }
} 
