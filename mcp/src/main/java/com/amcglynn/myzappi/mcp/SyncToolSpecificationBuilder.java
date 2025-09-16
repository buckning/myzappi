package com.amcglynn.myzappi.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.stream.Collectors;

public class SyncToolSpecificationBuilder {
    public McpServerFeatures.SyncToolSpecification getToolSpecification(McpTool mcpTool) {
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(mcpTool.getName(), mcpTool.getDescription(),
                        mcpTool.getSchema()), mcpTool.getToolImplementation());
    }

    public List<McpServerFeatures.SyncToolSpecification> getToolSpecifications(List<McpTool> mcpTools) {
        return mcpTools.stream()
                .map(this::getToolSpecification)
                .collect(Collectors.toList());
    }
}
