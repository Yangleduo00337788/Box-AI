package com.boxai.security.filter;

import com.boxai.common.constant.HeaderNames;
import com.boxai.common.constant.RequestAttributes;
import com.boxai.security.logging.LoggingContext;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesRequestIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(LoggingContext.REQUEST_ID)).startsWith("req_");
            assertThat(req.getAttribute(RequestAttributes.REQUEST_ID)).asString().startsWith("req_");
        });

        assertThat(response.getHeader(HeaderNames.REQUEST_ID)).startsWith("req_");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void preservesIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HeaderNames.REQUEST_ID, "req_client123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(MDC.get(LoggingContext.REQUEST_ID)).isEqualTo("req_client123");
        });

        assertThat(response.getHeader(HeaderNames.REQUEST_ID)).isEqualTo("req_client123");
    }
}
