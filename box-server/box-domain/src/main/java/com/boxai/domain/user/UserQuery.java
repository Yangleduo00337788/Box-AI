package com.boxai.domain.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserQuery {

    private String keyword;
    private String userType;
    private Integer status;
    private int page = 1;
    private int pageSize = 20;
}
