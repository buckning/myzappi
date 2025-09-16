package com.amcglynn.myzappi.mcp.tools.zappi;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.mcp.McpTool;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;
import java.util.function.BiFunction;

public class SetChargeMode implements McpTool {
    private ZappiService zappiService;

    public SetChargeMode(ZappiService zappiService) {
        this.zappiService = zappiService;
    }

    @Override
    public String getSchema() {
        return """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "chargeMode" : {
                  "type" : "string"
                }
              }
            }
            """;
    }

    @Override
    public String getName() {
        return "setChargeMode";
    }

    @Override
    public String getDescription() {
        return "Control myenergi zappi (car/electric vehicle (EV) charger. " +
                "The charge mode can be set to ECO, ECO+, Stop and Fast modes.";
    }

    public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getToolImplementation() {
        return (exchange, arguments) -> {
            var mode = (String) arguments.get("chargeMode");

            var result = "Setting the charge mode to: " + mode + ". This may take a few minutes to take effect.";
            zappiService.setChargeMode(ZappiChargeMode.valueOf((mode.toUpperCase())));
            return new McpSchema.CallToolResult(result, false);
        };
    }
}
