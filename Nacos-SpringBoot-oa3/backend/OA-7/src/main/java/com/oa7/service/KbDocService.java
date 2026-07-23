package com.oa7.service;

import com.oa7.pojo.KbDoc;
import com.oa7.util.RESP;

public interface KbDocService {

    RESP list(int currentPage, int pageSize, String keyword);

    RESP add(KbDoc doc, int currentPage, int pageSize, String keyword);

    RESP update(KbDoc doc, int currentPage, int pageSize, String keyword);

    RESP delete(Integer id, int currentPage, int pageSize, String keyword);

    RESP reloadIndex();
}
