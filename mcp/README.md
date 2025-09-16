# MCP Module

This module provides an MCP (Model Context Protocol) server for myenergi device control.
This is STDIO MCP server implementation, meaning that it communicates via standard input and output streams.

## Usage

This can be referenced in an MCP client, like VS Code, with the following configuration in mcp.json:
```
{
  "servers": {
    "myenergi": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-Xmx2G",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "-jar",
        "/path_to_project/myzappi/mcp/target/libs/mcp.jar"
      ],
      "env": {
        "API_KEY": <insert myenergi api key here>,
        "SERIAL_NUMBER": <insert myenergi hub serial number here>
      }
    }
  },
  "inputs": [
    {
      "id": "chargeMode",
      "type": "promptString",
      "description": "Sets the charge mode"
    }
  ]
}
```

Once this configuration is in place, you can start the MCP server from the command palette (cmd+p)
in VS Code and typing `> MCP: List Servers` and choosing `myenergi`, then `> Start Server`.

You should then see the MCP server starting in the output panel, similar to logs below:
```
2025-09-16 21:55:20.502 [info] Starting server myenergi
2025-09-16 21:55:20.504 [info] Connection state: Starting
2025-09-16 21:55:20.512 [info] Starting server from LocalProcess extension host
2025-09-16 21:55:20.513 [info] Connection state: Starting
2025-09-16 21:55:20.514 [info] Connection state: Running
2025-09-16 21:55:20.856 [info] Discovered 3 tools
```

You can then interact with the myenergi devices via the MCP client.

### Execute Tools
You can execute a tool by typing a command into the chat window. For example, you can type:
```
Get the charge mode
```

This realise that it needs to call the `getChargeMode` tool, but it will prompt you to confirm the action, something like:
```
Run getChargeMode - myenergi (MCP Server)
Get the charge mode for the myenergi zappi (car/electric vehicle (EV) charger.

Note that MCP servers or malicious conversation content may attempt to misuse 'Code' through tools.
```
You will be given the option to `Continue` or `Cancel`. If you choose `Continue`, the command will be sent to the MCP server, which will execute it and return the result.

The result will be displayed in the chat window, something like:
```
The current charge mode is set to ECO. If you need to change it or want more details, let me know!
```

### Prompt Templates
You can see the prompt templates by typing `/` into the chat window.
You will see a list of available prompts, including:
```
/mcp.myenergi.setChargeMode
```
It will then prompt you for the charge mode to set. You can enter one of the following values: ECO, ECO_PLUS, FAST, STOP.
Once this is done, the full prompt will be in the chat window, ready for you to send to the LLM, in this case:
```
Set charge mode to ECO
```

Once entered, the MCP client will confirm if you wish to run the command. It will present something like:
```
Run setChargeMode - myenergi (MCP Server)
Control myenergi zappi (car/electric vehicle (EV) charger. The charge mode can be set to ECO, ECO+, Stop and Fast modes.

Input
Note that MCP servers or malicious conversation content may attempt to misuse 'Code' through tools.
```
If you confirm, the command will be sent to the MCP server, which will execute it and return the result.
```
The charge mode is being set to ECO. It may take a few minutes for the change to take effect. Let me know if you need to check the status or make further adjustments.
```
## Building

The module is built as part of the main project build:

```
./gradlew clean build
```

This will create a fat JAR containing all dependencies in `mcp/target/mcp.jar`.
