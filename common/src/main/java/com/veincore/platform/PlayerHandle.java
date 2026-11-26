package com.veincore.platform;

/** 玩家句柄：common 只认字符串 id，平台层负责 UUID/名称解析。 */
public record PlayerHandle(String id) {
}
