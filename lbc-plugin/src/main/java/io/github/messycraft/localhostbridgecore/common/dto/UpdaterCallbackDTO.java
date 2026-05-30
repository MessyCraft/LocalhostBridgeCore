package io.github.messycraft.localhostbridgecore.common.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@RequiredArgsConstructor
public class UpdaterCallbackDTO {

    /**
     * 子服是否禁用了 Updater
     */
    private boolean disabled;

    /**
     * 是否执行了更新
     */
    private final boolean hasUpdate;

    /**
     * 是否准备重启
     */
    private final boolean hasReboot;

    /**
     * 执行更新了的配置文件名列表
     */
    private final List<String> updatedConfigFiles;

    /**
     * 执行更新了的插件Jar名列表
     */
    private final List<String> updatedPluginFiles;

    public UpdaterCallbackDTO() {
        disabled = true;
        hasUpdate = false;
        hasReboot = false;
        updatedConfigFiles = Collections.emptyList();
        updatedPluginFiles = Collections.emptyList();
    }

}
