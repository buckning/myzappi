package com.amcglynn.myzappi.login;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.model.CompleteLoginState;
import com.amcglynn.myzappi.core.service.LoginCode;
import com.amcglynn.myzappi.core.service.LoginService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;

public class CompleteLoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private LoginService loginService;

    public CompleteLoginHandler() {
        var serviceManager = new ServiceManager(new Properties());
        this.loginService = serviceManager.getLoginService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        if ("GET".equals(input.getHttpMethod())) {
            response.setStatusCode(200);
            response.setHeaders(Collections.singletonMap("Content-Type", "text/html"));
            response.setBody(htmlContent);
            return response;
        }

        try {
            var body = new ObjectMapper().readValue(input.getBody(), new TypeReference<CompleteLoginRequest>(){});

            var loginCode = body.getLoginCode().replaceAll("\\s","").toLowerCase();

            var result = loginService.completeLogin(LoginCode.from(loginCode), body.getApiKey().trim());
            if (CompleteLoginState.COMPLETE.equals(result.getState())) {
                response.setStatusCode(202);
                response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            } else {
                response.setStatusCode(404);
                response.setHeaders(Collections.singletonMap("Content-Type", "application/json"));
            }

        } catch (JsonProcessingException e) {
            response.setStatusCode(400);
            e.printStackTrace();
        }

        return response;
    }

    private String htmlContent = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>My Zappi</title>\n" +
            "    <style>\n" +
            "        body {\n" +
            "            font-family: Arial, sans-serif;\n" +
            "            background-color: #f2f2f2;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "\n" +
            "        h1 {\n" +
            "            text-align: center;\n" +
            "            color: #333;\n" +
            "        }\n" +
            "\n" +
            "        form {\n" +
            "            max-width: 400px;\n" +
            "            margin: 0 auto;\n" +
            "            background-color: #fff;\n" +
            "            padding: 20px;\n" +
            "            border-radius: 4px;\n" +
            "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\n" +
            "        }\n" +
            "\n" +
            "        label {\n" +
            "            display: block;\n" +
            "            margin-bottom: 10px;\n" +
            "        }\n" +
            "\n" +
            "        input[type=\"text\"] {\n" +
            "            width: 380px;\n" +
            "            padding: 10px;\n" +
            "            padding-right: 10px;\n" +
            "            font-size: 14px;\n" +
            "            border-radius: 4px;\n" +
            "            border: 1px solid #ccc;\n" +
            "            margin-bottom: 10px; /* Added vertical spacing */\n" +
            "        }\n" +
            "\n" +
            "        button {\n" +
            "            background-color: #4CAF50;\n" +
            "            color: #fff;\n" +
            "            padding: 10px 20px;\n" +
            "            font-size: 16px;\n" +
            "            border: none;\n" +
            "            border-radius: 4px;\n" +
            "            cursor: pointer;\n" +
            "        }\n" +
            "\n" +
            "        button:disabled {\n" +
            "            background-color: #ccc;\n" +
            "            cursor: not-allowed;\n" +
            "        }\n" +
            "\n" +
            "        .message {\n" +
            "            margin-top: 10px;\n" +
            "            padding: 5px;\n" +
            "            border-radius: 5px;\n" +
            "            max-width: 400px;\n" +
            "            margin: 0 auto;\n" +
            "        }\n" +
            "\n" +
            "        .success {\n" +
            "            background-color: #d4edda;\n" +
            "            color: #155724;\n" +
            "        }\n" +
            "\n" +
            "        .error {\n" +
            "            background-color: #f8d7da;\n" +
            "            color: #721c24;\n" +
            "        }\n" +
            "\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>Welcome to My Zappi!</h1>\n" +
            "    <form id=\"otpForm\">\n" +
            "        <div>\n" +
            "            <label for=\"otp\">My Zappi Code:</label>\n" +
            "            <input type=\"text\" id=\"otp\" name=\"otp\" required>\n" +
            "        </div>\n" +
            "        <div>\n" +
            "            <label for=\"apiKey\">Zappi API Key:</label>\n" +
            "            <input type=\"text\" id=\"apiKey\" name=\"apiKey\" required>\n" +
            "        </div>\n" +
            "        <div>\n" +
            "            <button type=\"button\" id=\"submitBtn\">Submit</button>\n" +
            "        </div>\n" +
            "    </form>\n" +
            "\n" +
            "    <div id=\"messageContainer\"></div>\n" +
            "\n" +
            "    <script>\n" +
            "        var submitButton = document.getElementById(\"submitBtn\");\n" +
            "        submitButton.addEventListener(\"click\", function(event) {\n" +
            "            event.preventDefault(); // Prevent form submission\n" +
            "\n" +
            "            // Retrieve the OTP and API key values from the form\n" +
            "            var otpValue = document.getElementById(\"otp\").value;\n" +
            "            var apiKeyValue = document.getElementById(\"apiKey\").value;\n" +
            "            \n" +
            "            submitButton.disabled = true;\n" +
            "            \n" +
            "            // Create the JSON object with the OTP and API key\n" +
            "            var requestBody = {\n" +
            "                \"loginCode\": otpValue,\n" +
            "                \"apiKey\": apiKeyValue\n" +
            "            };\n" +
            "\n" +
            "            // Make the POST request to http://localhost:8080/otp\n" +
            "            fetch(\"https://uevnoh4hxi.execute-api.eu-west-1.amazonaws.com/default/myzappi-login\", {\n" +
            "                method: \"POST\",\n" +
            "                headers: {\n" +
            "                    \"Content-Type\": \"application/json\"\n" +
            "                },\n" +
            "                body: JSON.stringify(requestBody)\n" +
            "            })\n" +
            "            .then(function(response) {\n" +
            "                \n" +
            "                if (response.ok) {\n" +
            "                    document.getElementById(\"otpForm\").style.display = \"none\";\n" +
            "                    showMessage(\"My Zappi is now configured. You can start using it on your Alexa device.\", \"success\");\n" +
            "                } else {\n" +
            "                    submitButton.disabled = false;\n" +
            "                    showMessage(\"Something went wrong. Please check you entered the correct My Zappi code.\", \"error\");\n" +
            "                }\n" +
            "            })\n" +
            "            .catch(function(error) {\n" +
            "                submitButton.disabled = false;\n" +
            "                showMessage(\"Something is very wrong, please try again later\", \"error\");\n" +
            "            });\n" +
            "        });\n" +
            "\n" +
            "        function showMessage(message, type) {\n" +
            "            var messageContainer = document.getElementById(\"messageContainer\");\n" +
            "            messageContainer.innerHTML = \"<div class='message \" + type + \"'>\" + message + \"</div>\";\n" +
            "        }\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>\n";
}
