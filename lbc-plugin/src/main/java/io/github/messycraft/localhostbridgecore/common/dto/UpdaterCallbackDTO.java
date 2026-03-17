package io.github.messycraft.localhostbridgecore.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UpdaterCallbackDTO {

    /**
     * 是否执行了更新
     */
    private boolean hasUpdate;

    /**
     * 是否准备重启
     */
    private boolean hasReboot;

    /**
     * 执行更新了的配置文件名列表
     */
    private List<String> updatedConfigFiles;

    /**
     * 执行更新了的插件Jar名列表
     */
    private List<String> updatedPluginFiles;

}
