package cn.zengcanxiang.packplugin.task

import cn.zengcanxiang.packplugin.PluginExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec

class CopySourceTask {
    private PluginExtension config
    private Project project

    CopySourceTask(Project project) {
        this.project = project
        config = PluginExtension.getConfig(project)
    }

    def copySource() {
        println("开始复制文件")
        def extension = project.extensions.getByName("android") as BaseExtension
        def versionName = extension.defaultConfig.versionName
        def versionCode = extension.defaultConfig.versionCode
        File out = new File(PluginExtension.channelOutputFolder, "${versionName}_${versionCode}_source")
        if(config.apkFilePath != null){
            File sourceApk = new File(config.apkFilePath)
            if (sourceApk.exists()) {
                println("开始复制apk")
                copy(sourceApk, out)
            }
        }
        if(config.mappingPath != null){
            File sourceMapping = new File(config.mappingPath)
            if (sourceMapping.exists()) {
                println("开始复制mapping")
                copy(sourceMapping, out)
            }
        }
    }

    private def copy(File source, File out) {
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.from(source)
                        .into(out)
            }
        })
    }
}
