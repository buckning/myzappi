package com.amcglynn.myzappi.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.function.BiFunction;

public interface McpTool {

    String getSchema();
    String getName();
    String getDescription();
    BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getToolImplementation();
}
