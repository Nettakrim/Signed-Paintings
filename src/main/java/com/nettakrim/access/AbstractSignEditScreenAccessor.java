package com.nettakrim.access;

public interface AbstractSignEditScreenAccessor {
    int paste(String s, int selectionStart, int selectionEnd);

    void clear();

    String getCombinedMessage();
}
