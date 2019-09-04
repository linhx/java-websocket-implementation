package org.lnd.chat.message;

public class MsgContent implements IContent<String> {
    String content;

    @Override
    public String getMessage() {
        return content;
    }
}
