package com.errorlog;

import java.util.Comparator;

public class ErrorComparators {

    public static Comparator<ServerError> byErrorType() {
        return new Comparator<ServerError>() {
            @Override
            public int compare(ServerError e1, ServerError e2) {
                return e1.getErrorType().compareTo(e2.getErrorType());
            }
        };
    }

    public static Comparator<ServerError> byErrorTypeDescending() {
        return new Comparator<ServerError>() {
            @Override
            public int compare(ServerError e1, ServerError e2) {
                return e2.getErrorType().compareTo(e1.getErrorType());
            }
        };
    }

    public static Comparator<ServerError> byErrorId() {
        return new Comparator<ServerError>() {
            @Override
            public int compare(ServerError e1, ServerError e2) {
                return Integer.compare(e1.getErrorId(), e2.getErrorId());
            }
        };
    }
}
