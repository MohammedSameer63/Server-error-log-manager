package com.errorlog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// Serializable allows HttpSession to persist this object across requests
public class ErrorLogManager implements Serializable {

    private ArrayList<ServerError> errorList;
    private int idCounter;

    public ErrorLogManager() {
        errorList = new ArrayList<>();
        idCounter = 1;
    }

    public void addLog(String rawLog) {
        if (rawLog == null || rawLog.trim().isEmpty()) {
            throw new IllegalArgumentException("Log entry cannot be empty.");
        }
        if (!rawLog.contains(":")) {
            throw new IllegalArgumentException(
                "Invalid format. Expected 'TYPE: message' — got: " + rawLog
            );
        }
        String type = rawLog.substring(0, rawLog.indexOf(":")).trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Error type cannot be empty.");
        }

        ServerError error = new ServerError(idCounter++, rawLog);
        errorList.add(error);
    }

    public ArrayList<ServerError> getAllErrors() {
        return errorList;
    }

    public ArrayList<ServerError> getCriticalErrors() {
        ArrayList<ServerError> criticalList = new ArrayList<>();
        for (ServerError error : errorList) {
            if (error.isCritical()) {
                criticalList.add(error);
            }
        }
        return criticalList;
    }

    public void sortBy(Comparator<ServerError> comparator) {
        Collections.sort(errorList, comparator);
    }

    public int countByType(String type) {
        int count = 0;
        for (ServerError e : errorList) {
            if (e.getErrorType().equalsIgnoreCase(type)) count++;
        }
        return count;
    }

    public void clearAll() {
        errorList.clear();
        idCounter = 1;
    }
}
