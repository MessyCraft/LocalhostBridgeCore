package io.github.messycraft.localhostbridgecore.bungee.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Deprecated
@Data
@RequiredArgsConstructor
public class LSession {

    private final String unique;
    private final String namespace;
    private final String seq;
    private final long millis;

}
