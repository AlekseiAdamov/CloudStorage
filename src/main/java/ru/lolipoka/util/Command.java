package ru.lolipoka.util;

public enum Command {
    HELP("help", 1, "\thelp\t\t\t- list all available commands (this information)\n\r"),
    LS("ls", 1, "\tls\t\t\t- view all files and directories\n\r"),
    MKDIR("mkdir [dirname]", 2, "\tmkdir [dirname]\t\t- create directory\n\r"),
    TOUCH("touch [filename]", 2, "\ttouch [filename]\t- create file\n\r"),
    CD("cd [path | .. | ~]", 2, "\tcd [path | .. | ~]\t- change directory to path,\n\r\t\t\t\t  parent (..) or root (~)\n\r"),
    RM("rm [filename | dirname]", 2, "\trm [filename | dirname]\t- delete file or directory\n\r"),
    COPY("copy [from] [to]", 3, "\tcopy [from] [to]\t- copy file or directory\n\r"),
    CAT("cat [filename]", 2, "\tcat [filename]\t\t- view file\n\r"),
    NICK("nick [new nick]", 2, "\tnick [new nick]\t\t- change nickname\n\r"),
    EXIT("exit", 1, "\texit\t\t\t- close connection, exit server\n\r");

    private final String syntax;
    private final int numOfParameters;
    private final String description;

    Command(String syntax, int numOfParameters, String description) {
        this.syntax = syntax;
        this.numOfParameters = numOfParameters;
        this.description = description;
    }

    public String getSyntax() {
        return syntax;
    }

    public int getNumOfParameters() {
        return numOfParameters;
    }

    public String getDescription() {
        return description;
    }
}
