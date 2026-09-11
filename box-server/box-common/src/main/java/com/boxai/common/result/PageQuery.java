package com.boxai.common.result;

public record PageQuery(Integer page, Integer pageSize) {

    public PageQuery {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
