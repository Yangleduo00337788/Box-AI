package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.tool.Tool;
import com.boxai.domain.tool.ToolDatabaseConfig;
import com.boxai.domain.tool.ToolDatabaseConfigRepository;
import com.boxai.domain.tool.ToolFunctionConfig;
import com.boxai.domain.tool.ToolFunctionConfigRepository;
import com.boxai.domain.tool.ToolHttpConfig;
import com.boxai.domain.tool.ToolHttpConfigRepository;
import com.boxai.tool.api.ToolTestResultVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ToolExecutionService {

    private final ToolHttpConfigRepository toolHttpConfigRepository;
    private final ToolDatabaseConfigRepository toolDatabaseConfigRepository;
    private final ToolFunctionConfigRepository toolFunctionConfigRepository;
    private final HttpToolExecutor httpToolExecutor;
    private final DatabaseToolExecutor databaseToolExecutor;
    private final ScriptToolExecutor scriptToolExecutor;

    public ToolExecutionService(ToolHttpConfigRepository toolHttpConfigRepository,
                                ToolDatabaseConfigRepository toolDatabaseConfigRepository,
                                ToolFunctionConfigRepository toolFunctionConfigRepository,
                                HttpToolExecutor httpToolExecutor,
                                DatabaseToolExecutor databaseToolExecutor,
                                ScriptToolExecutor scriptToolExecutor) {
        this.toolHttpConfigRepository = toolHttpConfigRepository;
        this.toolDatabaseConfigRepository = toolDatabaseConfigRepository;
        this.toolFunctionConfigRepository = toolFunctionConfigRepository;
        this.httpToolExecutor = httpToolExecutor;
        this.databaseToolExecutor = databaseToolExecutor;
        this.scriptToolExecutor = scriptToolExecutor;
    }

    public String execute(Tool tool, Map<String, Object> arguments) {
        String type = tool.getType() == null ? "" : tool.getType().trim().toUpperCase();
        return switch (type) {
            case "HTTP" -> executeHttp(tool, arguments);
            case "DATABASE" -> executeDatabase(tool, arguments);
            case "FUNCTION", "CODE" -> executeScript(tool, arguments);
            case "MCP" -> throw new BusinessException(ErrorCode.BAD_REQUEST, "MCP 工具请通过 MCP Server 绑定使用");
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的工具类型: " + type);
        };
    }

    public ToolTestResultVO test(Tool tool, String sql, Map<String, Object> arguments) {
        long started = System.currentTimeMillis();
        String type = tool.getType() == null ? "" : tool.getType().trim().toUpperCase();
        return switch (type) {
            case "HTTP" -> {
                ToolHttpConfig config = toolHttpConfigRepository.findByToolId(tool.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具未配置"));
                yield httpToolExecutor.execute(config);
            }
            case "DATABASE" -> {
                if (sql == null || sql.isBlank()) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "测试数据库工具需要提供 sql");
                }
                ToolDatabaseConfig config = toolDatabaseConfigRepository.findByToolId(tool.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "数据库工具未配置"));
                String body = databaseToolExecutor.execute(config, sql);
                yield new ToolTestResultVO(200, body, System.currentTimeMillis() - started);
            }
            case "FUNCTION", "CODE" -> {
                ToolFunctionConfig config = toolFunctionConfigRepository.findByToolId(tool.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "脚本工具未配置"));
                String body = scriptToolExecutor.execute(config, arguments == null ? Map.of() : arguments);
                yield new ToolTestResultVO(200, body, System.currentTimeMillis() - started);
            }
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "当前工具类型不支持测试: " + type);
        };
    }

    private String executeHttp(Tool tool, Map<String, Object> arguments) {
        ToolHttpConfig config = toolHttpConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "HTTP 工具未配置"));
        ToolTestResultVO result = httpToolExecutor.execute(config);
        return result.body() == null ? "" : result.body();
    }

    private String executeDatabase(Tool tool, Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        Object sqlValue = args.containsKey("sql") ? args.get("sql") : args.get("query");
        if (sqlValue == null || String.valueOf(sqlValue).isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据库工具调用缺少 sql 参数");
        }
        ToolDatabaseConfig config = toolDatabaseConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "数据库工具未配置"));
        return databaseToolExecutor.execute(config, String.valueOf(sqlValue));
    }

    private String executeScript(Tool tool, Map<String, Object> arguments) {
        ToolFunctionConfig config = toolFunctionConfigRepository.findByToolId(tool.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "脚本工具未配置"));
        return scriptToolExecutor.execute(config, arguments == null ? Map.of() : arguments);
    }
}
