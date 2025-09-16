package com.amcglynn.myzappi.mcp.tools;

import com.amcglynn.myenergi.MyEnergiClient;
import com.amcglynn.myenergi.apiresponse.MyEnergiDeviceStatus;
import com.amcglynn.myenergi.apiresponse.StatusResponse;
import com.amcglynn.myzappi.core.model.EddiDevice;
import com.amcglynn.myzappi.core.model.LibbiDevice;
import com.amcglynn.myzappi.core.model.MyEnergiDevice;
import com.amcglynn.myzappi.core.model.SerialNumber;
import com.amcglynn.myzappi.core.model.ZappiDevice;
import com.amcglynn.myzappi.mcp.McpTool;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@AllArgsConstructor
public class DiscoverDevices implements McpTool {

    private MyEnergiClient client;

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
        return "discoverDevices";
    }

    @Override
    public String getDescription() {
        return """
                Discover the myenergi devices. These are the different devices that can be controlled
                by other tools: Zappi (Electric Vehicle charger), Libbi (Battery) and Eddi (Water heater).
                Each myenergi device is identified by its serial number and device class.
                """;
    }

    public BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> getToolImplementation() {
        return (exchange, arguments) -> {
            var devices = getMyEnergiDevices(client.getStatus());
            var deviceInfo = devices.stream()
                    .map(device -> String.format("Serial: %s, Device Class: %s",
                            device.getSerialNumber().toString(),
                            device.getDeviceClass().toString()))
                    .reduce("", (acc, deviceStr) -> acc.isEmpty() ? deviceStr : acc + "\n" + deviceStr);

            if (deviceInfo.isEmpty()) {
                deviceInfo = "No devices found";
            }

            return new McpSchema.CallToolResult(deviceInfo, false);
        };
    }

    private List<MyEnergiDevice> getMyEnergiDevices(List<StatusResponse> statusResponse) {
        return statusResponse.stream()
                .map(this::getDevices)
                .flatMap(List::stream)
                .toList();
    }

    private List<MyEnergiDevice> getDevices(StatusResponse statusResponse) {
        if (statusResponse.getZappi() != null) {
            return statusResponse.getZappi()
                    .stream()
                    .map(this::toZappiDevice)
                    .toList();
        }
        if (statusResponse.getEddi() != null) {
            return statusResponse.getEddi()
                    .stream()
                    .map(this::toEddiDevice)
                    .toList();
        }
        if (statusResponse.getLibbi() != null) {
            return statusResponse.getLibbi()
                    .stream()
                    .map(this::toLibbiDevice)
                    .toList();
        }
        return List.of();
    }

    private MyEnergiDevice toZappiDevice(MyEnergiDeviceStatus status) {
        return new ZappiDevice(SerialNumber.from(status.getSerialNumber()));
    }

    private MyEnergiDevice toEddiDevice(MyEnergiDeviceStatus status) {
        return new EddiDevice(SerialNumber.from(status.getSerialNumber()), status.getTank1Name(), status.getTank2Name());
    }

    private MyEnergiDevice toLibbiDevice(MyEnergiDeviceStatus status) {
        return new LibbiDevice(SerialNumber.from(status.getSerialNumber()));
    }
}
