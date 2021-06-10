package ru.alekseiadamov.cloudstorage.server.util;

public class Command {
    public static final String AUTH = "/auth";
    public static final String AUTH_OK = "/authOk";
    public static final String AUTH_FAIL = "/authFail";
    public static final String UPLOAD = "/upload";
    public static final String DOWNLOAD = "/download";
    public static final String COPY = "/copy";
    public static final String COPY_OK = "/copyOk";
    public static final String COPY_FAIL = "/copyFail";
    public static final String MKDIR = "/mkdir";
    public static final String MKDIR_OK = "/mkdirOk";
    public static final String MKDIR_FAIL = "/mkdirFail";
    public static final String DELETE = "/delete";
    public static final String DELETE_OK = "/deleteOk";
    public static final String DELETE_FAIL = "/deleteFail";
    public static final String GRANT_PERMISSIONS = "/grantPermissions";
    public static final String DISCONNECT = "/disconnect";
    public static final String GET_DIR = "/getDir";
}
