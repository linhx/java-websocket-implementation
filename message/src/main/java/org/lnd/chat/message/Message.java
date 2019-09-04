package org.lnd.chat.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message extends AMessage {
    MsgContent content;
}
