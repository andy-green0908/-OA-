package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.KbDocDao;
import com.oa7.pojo.KbDoc;
import com.oa7.service.KbDocService;
import com.oa7.util.RESP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class KbDocServiceImpl implements KbDocService {

    private static final Logger log = LoggerFactory.getLogger(KbDocServiceImpl.class);

    @Autowired
    private KbDocDao kbDocDao;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oa.ai-service.base-url:http://127.0.0.1:18083/api/v1/ai}")
    private String aiServiceBaseUrl;

    @Value("${oa.internal-token}")
    private String internalToken;

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize, String keyword) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        PageHelper.startPage(currentPage, pageSize);
        List<KbDoc> docs = kbDocDao.selectPage(normalize(keyword));
        PageInfo<KbDoc> pageInfo = new PageInfo<>(docs);
        return RESP.ok(pageInfo.getList(), currentPage, (int) pageInfo.getTotal());
    }

    @Override
    public RESP add(KbDoc doc, int currentPage, int pageSize, String keyword) {
        if (!valid(doc)) {
            return RESP.error(400, "问题和答案不能为空");
        }
        normalizeDoc(doc);
        int rows = kbDocDao.insert(doc);
        if (rows <= 0) {
            return RESP.error("添加知识库条目失败");
        }
        reloadIndex();
        return list(currentPage, pageSize, keyword);
    }

    @Override
    public RESP update(KbDoc doc, int currentPage, int pageSize, String keyword) {
        if (doc == null || doc.getId() == null || !valid(doc)) {
            return RESP.error(400, "知识库参数不完整");
        }
        if (kbDocDao.selectById(doc.getId()) == null) {
            return RESP.error(404, "知识库条目不存在");
        }
        normalizeDoc(doc);
        int rows = kbDocDao.update(doc);
        if (rows <= 0) {
            return RESP.error("更新知识库条目失败");
        }
        reloadIndex();
        return list(currentPage, pageSize, keyword);
    }

    @Override
    public RESP delete(Integer id, int currentPage, int pageSize, String keyword) {
        if (id == null) {
            return RESP.error(400, "知识库 ID 不能为空");
        }
        int rows = kbDocDao.deleteById(id);
        if (rows <= 0) {
            return RESP.error(404, "知识库条目不存在");
        }
        reloadIndex();
        return list(currentPage, pageSize, keyword);
    }

    @Override
    public RESP reloadIndex() {
        try {
            String url = aiServiceBaseUrl + "/kb/reload";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);
            HttpEntity<Void> request = new HttpEntity<>(null, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return RESP.error("AI 服务知识库重载失败");
            }
            return RESP.ok("AI 服务知识库重载成功");
        } catch (Exception e) {
            log.warn("通知 AI 服务重载知识库失败: {}", e.getMessage());
            return RESP.error("通知 AI 服务重载知识库失败");
        }
    }

    private boolean valid(KbDoc doc) {
        return doc != null
                && doc.getQuestion() != null && !doc.getQuestion().trim().isEmpty()
                && doc.getAnswer() != null && !doc.getAnswer().trim().isEmpty();
    }

    private void normalizeDoc(KbDoc doc) {
        doc.setQuestion(doc.getQuestion().trim());
        doc.setAnswer(doc.getAnswer().trim());
        doc.setKeywords(normalize(doc.getKeywords()));
        if (doc.getHot() == null) {
            doc.setHot(false);
        }
        if (doc.getEnabled() == null) {
            doc.setEnabled(true);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
