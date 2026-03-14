package com.ron.match.server;

import com.ron.match.match.MatchState;

/**
 * 服务端单例匹配状态
 */
public final class MatchServerState {
    private static final MatchState STATE = new MatchState();

    public static MatchState get() {
        return STATE;
    }
}
