package com.amcglynn.myenergi.apiresponse;

import com.amcglynn.myenergi.units.Joule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZappiHistory {
    @JsonProperty("yr")
    private int year;

    @JsonProperty("dom")
    private int dayOfMonth;

    @JsonProperty("mon")
    private int month;

    @JsonProperty("hr")
    private Integer hour = 0;

    @JsonProperty("min")
    private Integer minute = 0;

    @JsonProperty("dow")
    private String dayOfWeek;

    @JsonProperty("gep")
    private Long solarGenerationJoules = 0L;     //Generated positive in Joules. gen is generated nevative

    @JsonProperty("exp")
    private Long gridExportJoules = 0L;

    @JsonProperty("h1b")
    private Long zappiBoostModeJoules = 0L;     // either manual or scheduled boost

    @JsonProperty("h1d")
    private Long zappiDivertedModeJoules = 0L;  // General usage either from PV or from the grid or both

    @JsonProperty("imp")
    private Long importedJoules = 0L;

    public ZappiHistory(int year, int dayOfMonth, int month, Integer hour, Integer minute, String dayOfWeek, Long solarGenerationJoules, Long gridExportJoules, Long zappiBoostModeJoules, Long zappiDivertedModeJoules, Long importedJoules) {
        this.year = year;
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.hour = hour;
        this.minute = minute;
        this.dayOfWeek = dayOfWeek;
        this.solarGenerationJoules = solarGenerationJoules;
        this.gridExportJoules = gridExportJoules;
        this.zappiBoostModeJoules = zappiBoostModeJoules;
        this.zappiDivertedModeJoules = zappiDivertedModeJoules;
        this.importedJoules = importedJoules;
    }

    public ZappiHistory() {
    }

    public static ZappiHistoryBuilder builder() {
        return new ZappiHistoryBuilder();
    }

    public Joule getGridExport() {
        return new Joule(gridExportJoules);
    }

    public Joule getImported() {
        return new Joule(importedJoules);
    }

    public Joule getBoost() {
        return new Joule(zappiBoostModeJoules);
    }

    public Joule getZappiDiverted() {
        return new Joule(zappiDivertedModeJoules);
    }

    public Joule getSolarGeneration() {
        return new Joule(solarGenerationJoules);
    }

    public String toString() {
        return "ZappiHistory(year=" + this.year + ", dayOfMonth=" + this.dayOfMonth + ", month=" + this.month + ", hour=" + this.hour + ", minute=" + this.minute + ", dayOfWeek=" + this.dayOfWeek + ", solarGenerationJoules=" + this.solarGenerationJoules + ", gridExportJoules=" + this.gridExportJoules + ", zappiBoostModeJoules=" + this.zappiBoostModeJoules + ", zappiDivertedModeJoules=" + this.zappiDivertedModeJoules + ", importedJoules=" + this.importedJoules + ")";
    }

    public int getYear() {
        return this.year;
    }

    public int getDayOfMonth() {
        return this.dayOfMonth;
    }

    public int getMonth() {
        return this.month;
    }

    public Integer getHour() {
        return this.hour;
    }

    public Integer getMinute() {
        return this.minute;
    }

    public static class ZappiHistoryBuilder {
        private int year;
        private int dayOfMonth;
        private int month;
        private Integer hour;
        private Integer minute;
        private String dayOfWeek;
        private Long solarGenerationJoules;
        private Long gridExportJoules;
        private Long zappiBoostModeJoules;
        private Long zappiDivertedModeJoules;
        private Long importedJoules;

        ZappiHistoryBuilder() {
        }

        public ZappiHistoryBuilder year(int year) {
            this.year = year;
            return this;
        }

        public ZappiHistoryBuilder dayOfMonth(int dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }

        public ZappiHistoryBuilder month(int month) {
            this.month = month;
            return this;
        }

        public ZappiHistoryBuilder hour(Integer hour) {
            this.hour = hour;
            return this;
        }

        public ZappiHistoryBuilder minute(Integer minute) {
            this.minute = minute;
            return this;
        }

        public ZappiHistoryBuilder dayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        public ZappiHistoryBuilder solarGenerationJoules(Long solarGenerationJoules) {
            this.solarGenerationJoules = solarGenerationJoules;
            return this;
        }

        public ZappiHistoryBuilder gridExportJoules(Long gridExportJoules) {
            this.gridExportJoules = gridExportJoules;
            return this;
        }

        public ZappiHistoryBuilder zappiBoostModeJoules(Long zappiBoostModeJoules) {
            this.zappiBoostModeJoules = zappiBoostModeJoules;
            return this;
        }

        public ZappiHistoryBuilder zappiDivertedModeJoules(Long zappiDivertedModeJoules) {
            this.zappiDivertedModeJoules = zappiDivertedModeJoules;
            return this;
        }

        public ZappiHistoryBuilder importedJoules(Long importedJoules) {
            this.importedJoules = importedJoules;
            return this;
        }

        public ZappiHistory build() {
            return new ZappiHistory(year, dayOfMonth, month, hour, minute, dayOfWeek, solarGenerationJoules, gridExportJoules, zappiBoostModeJoules, zappiDivertedModeJoules, importedJoules);
        }

        public String toString() {
            return "ZappiHistory.ZappiHistoryBuilder(year=" + this.year + ", dayOfMonth=" + this.dayOfMonth + ", month=" + this.month + ", hour=" + this.hour + ", minute=" + this.minute + ", dayOfWeek=" + this.dayOfWeek + ", solarGenerationJoules=" + this.solarGenerationJoules + ", gridExportJoules=" + this.gridExportJoules + ", zappiBoostModeJoules=" + this.zappiBoostModeJoules + ", zappiDivertedModeJoules=" + this.zappiDivertedModeJoules + ", importedJoules=" + this.importedJoules + ")";
        }
    }
}
