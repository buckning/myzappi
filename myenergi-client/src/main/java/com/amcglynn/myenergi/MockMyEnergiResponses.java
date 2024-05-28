package com.amcglynn.myenergi;

/**
 * Hub: 12345678
 * Zappis: 10000001, 10000002, 10000003
 * Eddis: 20000001, 20000002, 20000003
 * Libbis: 30000001, 30000002, 30000003
 */
public class MockMyEnergiResponses {

    private static final String ZAPPI_SERIAL_NUMBER_1 = "10000001";
    private static final String ZAPPI_SERIAL_NUMBER_2 = "10000002";
    private static final String EDDI_SERIAL_NUMBER_1 = "20000001";
    private static final String LIBBI_SERIAL_NUMBER_1 = "30000001";
    private static final String HARVI_SERIAL_NUMBER_1 = "40000001";

    private MockMyEnergiResponses() {}

    public static String getErrorResponse() {
        return "{\"status\":\"-14\",\"statustext\":\"\"}";
    }

    public static String getGenericResponse() {
        return "{\"status\":0,\"statustext\":\"\",\"asn\":\"s18.myenergi.net\"}";
    }

    public static String getExampleJStatusResponseWithZappiEddiAndLibbiButNoHarvi() {
        return String.format(""" 
                [
                {
                    "eddi": [
                        {
                            "deviceClass": "EDDI",
                            "sno": %s,
                            "dat": "10-05-2024",
                            "tim": "15:19:32",
                            "ectp1": 4,
                            "ectp2": 0,
                            "ectp3": 0,
                            "ectt1": "Internal Load",
                            "ectt2": "None",
                            "ectt3": "None",
                            "bsm": 0,
                            "bst": 0,
                            "dst": 1,
                            "div": 4,
                            "frq": 50.02,
                            "gen": 2723,
                            "grd": -76,
                            "pha": 1,
                            "pri": 3,
                            "sta": 1,
                            "tz": 0,
                            "vol": 2443,
                            "che": 0,
                            "isVHubEnabled": false,
                            "hpri": 1,
                            "hno": 1,
                            "ht1": "Tank 1",
                            "ht2": "Tank 2",
                            "r1a": 0,
                            "r2a": 0,
                            "r1b": 0,
                            "r2b": 0,
                            "rbc": 1,
                            "tp1": 42,
                            "tp2": 41,
                            "batteryDischargeEnabled": false,
                            "g100LockoutState": "NONE",
                            "cmt": 254,
                            "fwv": "3202S5.408",
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "productCode": "3202"
                        }
                    ]
                }
                ,
                {
                    "harvi": []
                }
                ,
                {
                    "libbi": [
                        {
                            "deviceClass": "LIBBI",
                            "sno": %s,
                            "dat": "10-05-2024",
                            "tim": "15:19:32",
                            "ectp1": 1936,
                            "ectp2": 70,
                            "ectp3": 1734,
                            "ectt1": "Internal Load",
                            "ectt2": "Grid",
                            "ectt3": "Generation",
                            "ectp4": 58,
                            "ectp5": 1027,
                            "ectt4": "Monitor",
                            "ectt5": "DCPV",
                            "ectt6": "None",
                            "dst": 1,
                            "tz": 0,
                            "lmo": "BALANCE",
                            "sta": 5,
                            "frq": 50.02,
                            "pri": 2,
                            "soc": 90,
                            "isp": true,
                            "pha": 1,
                            "vol": 2396,
                            "mbc": 10200,
                            "mic": 5000,
                            "gen": 2723,
                            "grd": -76,
                            "div": 1936,
                            "ect1p": 1,
                            "ect2p": 1,
                            "ect3p": 1,
                            "batteryDischargingBoost": false,
                            "pvDirectlyConnected": true,
                            "g100LockoutState": "NONE",
                            "countryCode": "GBR",
                            "isVHubEnabled": true,
                            "cmt": 254,
                            "fwv": "3702S5.433",
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "productCode": "3702"
                        }
                    ]
                }
                ,
                {
                    "zappi": [
                        {
                            "deviceClass": "ZAPPI",
                            "sno": %s,
                            "dat": "10-05-2024",
                            "tim": "15:19:32",
                            "ectp1": 0,
                            "ectp2": 6,
                            "ectp3": 0,
                            "ectt1": "Internal Load",
                            "ectt2": "Monitor",
                            "ectt3": "None",
                            "bsm": 0,
                            "bst": 0,
                            "dst": 1,
                            "div": 0,
                            "frq": 50.02,
                            "gen": 2723,
                            "grd": -76,
                            "pha": 1,
                            "pri": 1,
                            "sta": 1,
                            "tz": 0,
                            "vol": 2442,
                            "che": 5.1,
                            "isVHubEnabled": false,
                            "bss": 0,
                            "lck": 16,
                            "pst": "A",
                            "zmo": 3,
                            "pwm": 5297,
                            "zs": 256,
                            "rdc": -6,
                            "rac": -3,
                            "rrac": -6,
                            "zsh": 1,
                            "ectt4": "None",
                            "ectt5": "None",
                            "ectt6": "None",
                            "beingTamperedWith": false,
                            "batteryDischargeEnabled": true,
                            "g100LockoutState": "NONE",
                            "phaseSetting": "SINGLE_PHASE",
                            "mgl": 100,
                            "sbh": 6,
                            "sbk": 99,
                            "fwv": "3562S5.434",
                            "cmt": 254,
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "productCode": "3562"
                        },
                        {
                            "deviceClass": "ZAPPI",
                            "sno": %s,
                            "dat": "10-05-2024",
                            "tim": "15:19:32",
                            "ectp1": 0,
                            "ectp2": 6,
                            "ectp3": 0,
                            "ectt1": "Internal Load",
                            "ectt2": "Monitor",
                            "ectt3": "None",
                            "bsm": 0,
                            "bst": 0,
                            "dst": 1,
                            "div": 50000,
                            "frq": 50.02,
                            "gen": 572003,
                            "grd": -76000,
                            "pha": 1,
                            "pri": 1,
                            "sta": 0,
                            "tz": 0,
                            "vol": 2442,
                            "che": 50.1,
                            "isVHubEnabled": false,
                            "bss": 0,
                            "lck": 16,
                            "pst": "A",
                            "zmo": 1,
                            "pwm": 5297,
                            "zs": 256,
                            "rdc": -6,
                            "rac": -3,
                            "rrac": -6,
                            "zsh": 1,
                            "ectt4": "None",
                            "ectt5": "None",
                            "ectt6": "None",
                            "beingTamperedWith": false,
                            "batteryDischargeEnabled": true,
                            "g100LockoutState": "NONE",
                            "phaseSetting": "SINGLE_PHASE",
                            "mgl": 100,
                            "sbh": 6,
                            "sbk": 99,
                            "fwv": "3562S5.434",
                            "cmt": 254,
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "productCode": "3562"
                        }
                    ]
                }
                ,
                {
                    "asn": "s18.myenergi.net",
                    "fwv": "3402S5.433",
                    "vhub": 1
                }
                ]
                """, EDDI_SERIAL_NUMBER_1, LIBBI_SERIAL_NUMBER_1, ZAPPI_SERIAL_NUMBER_1, ZAPPI_SERIAL_NUMBER_2);
    }

    public static String getExampleResponse() {
        return String.format("""
                {
                    "zappi": [
                        {
                            "sno": %s,
                            "dat": "16-02-2023",
                            "tim": "11:47:14",
                            "ectp1": 0,
                            "ectp2": 54,
                            "ectp3": 0,
                            "ectt1": "Internal Load",
                            "ectt2": "Grid",
                            "ectt3": "None",
                            "bsm": 0,
                            "bst": 0,
                            "cmt": 254,
                            "dst": 1,
                            "div": 0,
                            "frq": 50.01,
                            "fwv": "3562S4.525",
                            "gen": 594,
                            "grd": 64,
                            "pha": 1,
                            "pri": 1,
                            "sta": 1,
                            "tz": 0,
                            "vol": 2389,
                            "che": 21.39,
                            "bss": 0,
                            "lck": 7,
                            "pst": "A",
                            "zmo": 3,
                            "pwm": 1200,
                            "zs": 256,
                            "rdc": -1,
                            "rac": 1,
                            "rrac": -3,
                            "zsh": 1,
                            "ectt4": "None",
                            "ectt5": "None",
                            "ectt6": "None",
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "beingTamperedWith": false,
                            "batteryDischargeEnabled": false,
                            "mgl": 30,
                            "sbh": 17,
                            "sbk": 5
                        }
                    ]
                }""", ZAPPI_SERIAL_NUMBER_1);
    }

    public static String getExampleLibbiResponse() {
        return String.format("""
                {
                    "libbi": [
                        {
                            "deviceClass": "LIBBI",
                            "sno": %s,
                            "dat": "10-05-2024",
                            "tim": "15:19:32",
                            "ectp1": 1936,
                            "ectp2": 70,
                            "ectp3": 1734,
                            "ectt1": "Internal Load",
                            "ectt2": "Grid",
                            "ectt3": "Generation",
                            "ectp4": 58,
                            "ectp5": 1027,
                            "ectt4": "Monitor",
                            "ectt5": "DCPV",
                            "ectt6": "None",
                            "dst": 1,
                            "tz": 0,
                            "lmo": "BALANCE",
                            "sta": 5,
                            "frq": 50.02,
                            "pri": 2,
                            "soc": 90,
                            "isp": true,
                            "pha": 1,
                            "vol": 2396,
                            "mbc": 10200,
                            "mic": 5000,
                            "gen": 2723,
                            "grd": -76,
                            "div": 1936,
                            "ect1p": 1,
                            "ect2p": 1,
                            "ect3p": 1,
                            "batteryDischargingBoost": false,
                            "pvDirectlyConnected": true,
                            "g100LockoutState": "NONE",
                            "countryCode": "GBR",
                            "isVHubEnabled": true,
                            "cmt": 254,
                            "fwv": "3702S5.433",
                            "newAppAvailable": false,
                            "newBootloaderAvailable": false,
                            "productCode": "3702"
                        }
                    ]
                }
                """, LIBBI_SERIAL_NUMBER_1);
    }

    public static String getExampleStatusResponse() {
        return String.format("""
                [
                    {
                        "eddi":[\s
                         {\s
                            "dat":"09-09-2019",
                            "tim":"16:55:50",
                            "ectp1":1,
                            "ectp2":1,
                            "ectt1":"Grid",
                            "ectt2":"Generation",
                            "frq":50.15,
                            "gen":304,
                            "grd":4429,
                            "hno":1,
                            "pha":3,
                            "sno":%s,
                            "sta":1,
                            "vol":0.0,
                            "ht1":"Tank 1",
                            "ht2":"Tank 2",
                            "tp1":-1,
                            "tp2":-1,
                            "pri":2,
                            "cmt":254,
                            "r1a":1,
                            "r2a":1,
                            "r2b":1,
                            "che":1
                         }
                      ]    },
                    {
                        "zappi": [
                            {
                                "sno": %s,
                                "dat": "08-07-2023",
                                "tim": "08:51:48",
                                "ectp1": 0,
                                "ectp2": 3239,
                                "ectp3": 0,
                                "ectt1": "Internal Load",
                                "ectt2": "Grid",
                                "ectt3": "None",
                                "bsm": 0,
                                "bst": 0,
                                "cmt": 254,
                                "dst": 1,
                                "div": 0,
                                "frq": 50.02,
                                "fwv": "3562S5.044",
                                "gen": 2254,
                                "grd": 3582,
                                "pha": 1,
                                "pri": 1,
                                "sta": 4,
                                "tz": 0,
                                "vol": 2286,
                                "bss": 0,
                                "lck": 7,
                                "pst": "A",
                                "zmo": 1,
                                "pwm": 1000,
                                "zs": 256,
                                "rdc": -1,
                                "rac": 3,
                                "rrac": -4,
                                "zsh": 1,
                                "ectt4": "None",
                                "ectt5": "None",
                                "ectt6": "None",
                                "newAppAvailable": false,
                                "newBootloaderAvailable": false,
                                "beingTamperedWith": false,
                                "batteryDischargeEnabled": false,
                                "g100LockoutState": "NONE",
                                "mgl": 30,
                                "sbh": 3,
                                "sbk": 99
                            }
                        ]
                    },
                    {
                        "harvi": [
                            {
                                "sno": %s,
                                "dat": "08-07-2023",
                                "tim": "08:51:48",
                                "ectp1": 2254,
                                "ectp2": 0,
                                "ectp3": 0,
                                "ectt1": "Generation",
                                "ectt2": "None",
                                "ectt3": "None",
                                "ect1p": 1,
                                "ect2p": 1,
                                "ect3p": 1,
                                "fwv": "3170S0.000"
                            }
                        ]
                    },
                    {
                        "libbi": []
                    },
                    {
                        "asn": "s18.myenergi.net",
                        "fwv": "3401S5.044",
                        "vhub": 1
                    }
                ]""", EDDI_SERIAL_NUMBER_1, ZAPPI_SERIAL_NUMBER_1, HARVI_SERIAL_NUMBER_1);
    }

    public static String getHistoryResponse() {
        return """
                {
                    "U20149781": [
                        {
                            "yr": 2023,
                            "mon": 1,
                            "dom": 20,
                            "dow": "Fri",
                            "imp": 173040,
                            "gen": 360,
                            "pect1": 171300,
                            "v1": 2412,
                            "frq": 5005
                        },
                        {
                            "yr": 2023,
                            "mon": 1,
                            "dom": 20,
                            "dow": "Fri",
                            "min": 1,
                            "imp": 36960,
                            "pect1": 37020,
                            "v1": 2434,
                            "frq": 5005
                        },
                        {
                            "yr": 2023,
                            "mon": 1,
                            "dom": 20,
                            "dow": "Fri",
                            "min": 2,
                            "imp": 37740,
                            "pect1": 37740,
                            "v1": 2433,
                            "frq": 5004
                        },
                        {
                            "yr": 2023,
                            "mon": 1,
                            "dom": 20,
                            "dow": "Fri",
                            "min": 3,
                            "imp": 37740,
                            "pect1": 37740,
                            "v1": 2437,
                            "frq": 5002
                        },
                        {
                            "yr": 2023,
                            "mon": 1,
                            "dom": 20,
                            "dow": "Fri",
                            "min": 4,
                            "imp": 37560,
                            "pect1": 37560,
                            "v1": 2429,
                            "frq": 5000
                        }
                    ]
                }""";
    }

    public static String getHourlyHistoryResponse() {
        return """
                {
                    "U12345678": [
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "imp": 1042680,
                            "gep": 900,
                            "gen": 1260,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 1,
                            "imp": 1017480,
                            "gep": 480,
                            "gen": 1740,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 2,
                            "imp": 1086360,
                            "gep": 300,
                            "gen": 1380,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 3,
                            "imp": 948180,
                            "gep": 1080,
                            "gen": 4800,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 4,
                            "imp": 976140,
                            "gep": 300,
                            "gen": 1980,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 5,
                            "imp": 576900,
                            "gep": 415680,
                            "gen": 1080,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 6,
                            "imp": 3720360,
                            "exp": 129720,
                            "gep": 1577880,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 7,
                            "imp": 3035040,
                            "exp": 730320,
                            "gep": 3446940,
                            "h1d": 647220,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 8,
                            "imp": 1100340,
                            "exp": 530220,
                            "gep": 4223940,
                            "h1d": 2896740,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 9,
                            "imp": 2297460,
                            "exp": 437760,
                            "gep": 4115580,
                            "h1d": 3323640,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 10,
                            "imp": 192600,
                            "exp": 604860,
                            "gep": 10543800,
                            "h1d": 8153880,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 11,
                            "imp": 1298700,
                            "exp": 2353140,
                            "gep": 7152840,
                            "h1d": 1368180,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 12,
                            "exp": 8722680,
                            "gep": 10226340,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 13,
                            "imp": 358080,
                            "exp": 976320,
                            "gep": 8334360,
                            "h1d": 5715780,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 14,
                            "imp": 887580,
                            "exp": 418860,
                            "gep": 5537220,
                            "h1d": 2338200,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 15,
                            "imp": 985860,
                            "exp": 418200,
                            "gep": 4686600,
                            "h1d": 4025040,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 16,
                            "imp": 1701600,
                            "exp": 174660,
                            "gep": 2464200,
                            "h1d": 1048380,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 17,
                            "imp": 1017900,
                            "gep": 895920,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 18,
                            "imp": 1460340,
                            "gep": 764880,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 19,
                            "imp": 3048660,
                            "gep": 552540,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 20,
                            "imp": 2626860,
                            "gep": 10740,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 21,
                            "imp": 4820220,
                            "gep": 480,
                            "gen": 1800,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 22,
                            "imp": 21387900,
                            "gep": 900,
                            "gen": 1680,
                            "h1b": 19815540,
                            "dow": "Sat"
                        },
                        {
                            "yr": 2023,
                            "mon": 5,
                            "dom": 20,
                            "hr": 23,
                            "imp": 28019520,
                            "gep": 480,
                            "gen": 3600,
                            "h1b": 26640840,
                            "dow": "Sat"
                        }
                    ]
                }""";
    }
}
