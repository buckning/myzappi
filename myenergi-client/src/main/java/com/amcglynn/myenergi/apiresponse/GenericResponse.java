package com.amcglynn.myenergi.apiresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericResponse {
    private int status;
    @JsonProperty("statustext")
    private String statusText;
    private String asn;

    public GenericResponse(int status, String statusText, String asn) {
        this.status = status;
        this.statusText = statusText;
        this.asn = asn;
    }

    public GenericResponse() {
    }

    public static GenericResponseBuilder builder() {
        return new GenericResponseBuilder();
    }

    public int getStatus() {
        return this.status;
    }

    public String getStatusText() {
        return this.statusText;
    }

    public String getAsn() {
        return this.asn;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @JsonProperty("statustext")
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public void setAsn(String asn) {
        this.asn = asn;
    }

    public static class GenericResponseBuilder {
        private int status;
        private String statusText;
        private String asn;

        GenericResponseBuilder() {
        }

        public GenericResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public GenericResponseBuilder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public GenericResponseBuilder asn(String asn) {
            this.asn = asn;
            return this;
        }

        public GenericResponse build() {
            return new GenericResponse(status, statusText, asn);
        }

        public String toString() {
            return "GenericResponse.GenericResponseBuilder(status=" + this.status + ", statusText=" + this.statusText + ", asn=" + this.asn + ")";
        }
    }
}
