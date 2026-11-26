package com.veincore.platform;

import com.veincore.config.ClientConfig;

/** 客户端配置访问抽象（两端各自读写 config/veincore-client.json）。 */
public interface IClientConfig {

    ClientConfig get();

    void save();
}
