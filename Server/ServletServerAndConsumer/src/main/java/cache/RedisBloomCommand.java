package cache;

import io.lettuce.core.protocol.ProtocolKeyword;

public enum RedisBloomCommand implements ProtocolKeyword {
    BF_RESERVE("BF.RESERVE"),
    BF_ADD("BF.ADD"),
    BF_MADD("BF.MADD"),
    BF_EXISTS("BF.EXISTS");

    private final byte[] bytes;
    private final String command;

    RedisBloomCommand(String command) {
        this.command = command;
        this.bytes = command.getBytes();
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return command;
    }
}