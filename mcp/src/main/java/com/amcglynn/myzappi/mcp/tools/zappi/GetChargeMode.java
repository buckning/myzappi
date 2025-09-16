package com.amcglynn.myzappi.mcp.tools.zappi;

import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.function.BiFunction;
import io.modelcontextprotocol.server.McpSyncServerExchange;

public class GetChargeMode implements McpTool {

    private final ZappiService zappiService;

    public GetChargeMode(ZappiService myEnergiClient) {
        this.zappiService = myEnergiClient;
    }

    @Override
    public String getSchema() {
        return """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:Operation"
                }
                """;
    }

    @Override
    public String getName() {
        return "getChargeMode";
    }

    @Override
    public String getDescription() {
        return "Get the charge mode for the myenergi zappi (car/electric vehicle (EV) charger.";
    }

    public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getToolImplementation() {
        return (exchange, arguments) -> {
            // Tool implementation
            var result = "Charge mode is set to: " +
                    zappiService.getStatusSummary().getFirst().getChargeMode();
            return new McpSchema.CallToolResult(result, false);
        };
    }
}
