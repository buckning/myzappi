package com.amcglynn.myenergi;

/**
 * Hub: 12345678
 * Zappis: 10000001, 10000002, 10000003
 * Eddis: 20000001, 20000002, 20000003
 * Libbis: 30000001, 30000002, 30000003
 */
public class MockMyEnergiOAuthResponses {
    
    private static final String LIBBI_SERIAL_NUMBER_1 = "30000001";

    private MockMyEnergiOAuthResponses() {}

    public static String getLibbiChargeSetupResponse() {
        return String.format("""
                {
                    "status":true,
                    "message":"",
                    "field":"",
                    "content":{
                        "deviceSerial":"%s",
                        "chargeFromGrid":false,
                        "energyTarget":5520
                    }
                }
                """, LIBBI_SERIAL_NUMBER_1);
    }


    public static String getSetLibbiChargeFromGridResponse() {
        return """
               {
                    "status": true,
                    "message": "",
                    "field": "",
                    "content": "Libbi mode flag successfully updated!"
                }
                """;
    }

    public static String getSetLibbiChargeFromGridErrorResponse() {
        return """
                {
                  "status": false,
                  "message": "Device not found or user does not have access to it!",
                  "field": ""
                }
                """;
    }
}
