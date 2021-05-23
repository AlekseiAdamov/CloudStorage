package ru.lolipoka.util;

public enum MsgTemplate {
    TOO_MUCH_PARAMETERS("Command syntax: %s. Excessive parameters are ignored.\n\r"),
    NOT_ENOUGH_PARAMETERS("Command syntax: %s. Not enough parameters.\n\r"),
    FILE_NOT_FOUND("File %s not found.\n\r"),
    UNABLE_TO_CREATE("Unable to create %s.\n\r"),
    UNABLE_TO_DELETE("Unable to delete %s.\n\r"),
    DIRECTORY_DOES_NOT_EXIST("Directory %s does not exist.\n\r"),
    IS_NOT_FILE("%s is not file. Unable to read.\n\r");

    private final String template;

    MsgTemplate(String template) {
        this.template = template;
    }

    public String getTemplate() {
        return template;
    }
}
