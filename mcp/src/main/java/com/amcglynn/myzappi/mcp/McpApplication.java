package com.amcglynn.myzappi.mcp;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myzappi.core.service.ZappiService;
import com.amcglynn.myzappi.mcp.tools.DiscoverDevices;
import com.amcglynn.myzappi.mcp.tools.zappi.GetChargeMode;
import com.amcglynn.myzappi.mcp.tools.zappi.SetChargeMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * Main entry point for the MCP (Master Control Program) application.
 */
public class McpApplication {

    public static void main(String[] args) {
        var serialNumber = System.getenv("SERIAL_NUMBER");
        var apiKey = System.getenv("API_KEY");

        var client = new MyEnergiClient(serialNumber, apiKey);
        var zappiService = new ZappiService(client);

        var toolsList = List.of(
                new DiscoverDevices(client),
                new GetChargeMode(zappiService),
                new SetChargeMode(zappiService));

        var provider = new StdioServerTransportProvider(new ObjectMapper());

        var prompt = new McpSchema.Prompt("setChargeMode", "Set the charge mode of a Zappi device", List.of(
                new McpSchema.PromptArgument("chargeMode", "What charge mode would you like (ECO, ECO+, STOP, FAST)?", true)
        ));
        var syncPromptSpecification = new McpServerFeatures.SyncPromptSpecification(
                prompt,
                (exchange, request) -> {
                    var chargeMode = request.arguments().get("chargeMode");
                    // Prompt implementation
                    var userMessage = new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent("Set charge mode to " + chargeMode));

                    return new McpSchema.GetPromptResult("Set the charge mode of a Zappi", List.of(userMessage));
                }
        );

        var server = McpServer.sync(provider)
                .serverInfo("myenergiMcpUnofficial", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities
                        .builder()
                        .tools(true)
                        .prompts(true)
                        .build())
                .tools(new SyncToolSpecificationBuilder().getToolSpecifications(toolsList))
                .prompts(List.of(syncPromptSpecification))
                .build();
    }
}
