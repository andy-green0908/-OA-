package com.oa7.controller;

import com.oa7.pojo.KbDoc;
import com.oa7.service.KbDocService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - AI 客服知识库管理
 *
 * 完整路由前缀：/api/v1/admin/kb-docs
 */
@RestController
@RequestMapping("/kb-docs")
@CrossOrigin
public class KbDocController {

    @Autowired
    private KbDocService kbDocService;

    @GetMapping
    public RESP list(@RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                     @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                     @RequestParam(name = "keyword", required = false) String keyword) {
        return kbDocService.list(currentPage, pageSize, keyword);
    }

    @PostMapping
    public RESP add(@RequestBody KbDoc doc,
                    @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                    @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                    @RequestParam(name = "keyword", required = false) String keyword) {
        return kbDocService.add(doc, currentPage, pageSize, keyword);
    }

    @PutMapping("/{id}")
    public RESP update(@PathVariable("id") Integer id,
                       @RequestBody KbDoc doc,
                       @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                       @RequestParam(name = "keyword", required = false) String keyword) {
        doc.setId(id);
        return kbDocService.update(doc, currentPage, pageSize, keyword);
    }

    @DeleteMapping("/{id}")
    public RESP delete(@PathVariable("id") Integer id,
                       @RequestParam(name = "currentPage", defaultValue = "1") int currentPage,
                       @RequestParam(name = "pageSize", defaultValue = "8") int pageSize,
                       @RequestParam(name = "keyword", required = false) String keyword) {
        return kbDocService.delete(id, currentPage, pageSize, keyword);
    }

    @PostMapping("/reload-index")
    public RESP reloadIndex() {
        return kbDocService.reloadIndex();
    }
}
