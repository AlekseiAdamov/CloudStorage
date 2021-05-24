package ru.lolipoka.util;

public enum Command {
    LS("ls", "\tls\t\t\t- view all files and directories\n\r"),
    MKDIR("mkdir [dirname]", "\tmkdir [dirname]\t\t- create directory\n\r"),
    TOUCH("touch [filename]", "\ttouch [filename]\t- create file\n\r"),
    CD("cd [path | .. | ~]", "\tcd [path | .. | ~]\t- change directory to path,\n\r\t\t\t\t  parent (..) or root (~)\n\r"),
    RM("rm [filename | dirname]", "\trm [filename | dirname]\t- delete file or directory\n\r"),
    COPY("copy [from] [to]", "\tcopy [from] [to]\t- copy file or directory\n\r"),
    CAT("cat [filename]", "\tcat [filename]\t\t- view file\n\r"),
    NICK("nick [new nick]", "\tnick [new nick]\t\t- change nickname\n\r");

    private final String description;
    private final String syntax;

    Command(String syntax, String description) {
        this.syntax = syntax;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getSyntax() {
        return syntax;
    }
}
