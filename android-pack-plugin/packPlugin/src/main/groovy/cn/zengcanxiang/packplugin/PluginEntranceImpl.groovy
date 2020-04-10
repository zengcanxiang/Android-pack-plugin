package cn.zengcanxiang.packplugin

import cn.zengcanxiang.packplugin.task.DownloadTask
import cn.zengcanxiang.packplugin.task.FirmTask
import cn.zengcanxiang.packplugin.task.MultiChannelTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginEntranceImpl implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create("androidPackPlugin", PluginExtension)
        def downloadTask = project.tasks.create("downTask", DownloadTask)
        def firmTask = project.tasks.create("firmTask", FirmTask)
        def multiChannelTask = project.tasks.create("multiChannelTask", MultiChannelTask)

        // 设置两个任务之间的依赖
        firmTask.dependsOn(downloadTask)
        multiChannelTask.dependsOn(firmTask)
    }
}
