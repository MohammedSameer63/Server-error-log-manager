package com.errorlog;

public class ServerError {

    private int errorId;
    private String errorType;
    private String message;

    public ServerError(int errorId, String rawLog) {
        this.errorId = errorId;

        int colonIndex = rawLog.indexOf(":");
        if (colonIndex != -1) {
            this.errorType = rawLog.substring(0, colonIndex).trim().toUpperCase();
            this.message   = rawLog.substring(colonIndex + 1).trim();
        } else {
            this.errorType = "UNKNOWN";
            this.message   = rawLog.trim();
        }
    }

    public int    getErrorId()   { return errorId; }
    public String getErrorType() { return errorType; }
    public String getMessage()   { return message; }

    public boolean isCritical() {
        return errorType.equalsIgnoreCase("CRITICAL");
    }

    @Override
    public String toString() {
        return "[" + errorId + "] " + errorType + ": " + message;
    }
}
