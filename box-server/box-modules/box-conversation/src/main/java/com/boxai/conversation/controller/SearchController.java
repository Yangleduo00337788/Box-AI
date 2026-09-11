package com.boxai.conversation.controller;

import com.boxai.common.result.Result;
import com.boxai.conversation.api.AnalyticsOverviewVO;
import com.boxai.conversation.api.SearchResultVO;
import com.boxai.conversation.application.AnalyticsApplicationService;
import com.boxai.conversation.application.SearchApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final SearchApplicationService searchApplicationService;

    public SearchController(SearchApplicationService searchApplicationService) {
        this.searchApplicationService = searchApplicationService;
    }

    @GetMapping("/api/v1/search")
    public Result<List<SearchResultVO>> search(@RequestParam("q") String q) {
        return Result.success(searchApplicationService.search(q));
    }
}
