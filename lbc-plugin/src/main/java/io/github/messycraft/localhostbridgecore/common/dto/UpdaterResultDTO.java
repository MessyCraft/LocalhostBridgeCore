package io.github.messycraft.localhostbridgecore.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdaterResultDTO {

    /**
     * 是否启用
     */
    private boolean enable;

    /**
     * 共享目录位置
     */
    private String sharedPath;

    /**
     * 是否替换时备份
     */
    private boolean backupOnReplace;

    /**
     * K - 插件名; V - 文件名
     */
    private Map<String, String> accessPlugins;

    /**
     * K - 插件名; V - 重载指令列表
     */
    private Map<String, List<String>> reloadConfigCommand;

    /**
     * K - 插件名; V - $$LBC_SERVER_NAME$$ 替换名
     */
    private Map<String, String> serverPlaceholderMapping;

}
