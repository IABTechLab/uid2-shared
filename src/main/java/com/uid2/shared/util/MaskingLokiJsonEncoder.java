package com.uid2.shared.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.loki4j.logback.JsonEncoder;

public class MaskingLokiJsonEncoder extends JsonEncoder {
    private MaskingPatternLayout maskingPatternLayout;
    private MessageCfg message = new MessageCfg();

    @Override
    public void start() {
        super.start();

        maskingPatternLayout = new MaskingPatternLayout();
        maskingPatternLayout.setContext(context);
        maskingPatternLayout.setPattern(message.pattern);
        maskingPatternLayout.start();
    }

    @Override
    public void stop() {
        super.stop();

        maskingPatternLayout.stop();
    }

    @Override
    public String eventToMessage(ILoggingEvent e) {
        return maskingPatternLayout.doLayout(e);
    }

    public void setMessage(MessageCfg message) {
        this.message = message;
    }

    public static final class MessageCfg {
        private String pattern = "l=%level c=%logger{20} t=%thread | %msg %ex";

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
    }
}
