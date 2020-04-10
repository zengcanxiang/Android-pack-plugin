package cn.zengcanxiang.packplugin.task

import cn.zengcanxiang.packplugin.PluginExtension
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class FirmTask extends DefaultTask {
    private PluginExtension config

    File jar
    File firmJarParentPath
    private def firmJarPath = "jiagu/jiagu.jar"

    FirmTask() {
        group = "android-pack"
        description = "执行360加固"
        config = PluginExtension.getConfig(project)
    }

    @TaskAction
    def firm() {
        firmJarParentPath = new File(config.outPath, "360")
        jar = new File(firmJarParentPath, firmJarPath)
        if (!config.isNeedFirm || !login()) {
            return
        }
        config.initFirm()
        clearFirmService()
        println("开始360加固")
        def firmResultPath = new File(new File(config.outPath, "firmResult"),
                new Date().format("yyyy_MM_dd_HH_mm_ss")
        )
        firmResultPath.mkdirs()
        def firmShell = "java -jar $jar.absolutePath -jiagu $config.apkFilePath $firmResultPath.absolutePath"
        def out = new StringBuilder(), err = new StringBuilder()
        // 10分钟的执行时间
        executeShell(firmShell, out, err, 1000 * 60 * 10)
        println("判断360加固是否完成")
        if (err.length() > 0) {
            println(err.toString())
            if (!err.contains("error=13, Permission denied")) {
                println("加固 失败")
                return
            }
        }
        if (out.length() <= 0 || !(out.contains("已加固") || out.contains("任务完成"))) {
            println("加固 验证成功条件不符合，可能存在失败情况")
            println(out.toString())
        }
        println("加固 完成")
        firmResultPath.eachFileMatch(FileType.FILES, ~/.*\.apk/) {
            PluginExtension.apkFile = it
        }
    }

    private Boolean login() {
        if (!jar.exists()) {
            def os = System.getProperty("os.name").toLowerCase()
            if (os.contains("linux")) {
                // 360加固linux 的文件夹里面的摆放和其他的不一样，需要处理
                firmJarParentPath.eachFile { child ->
                    "mv ${new File(child, "jiagu").absolutePath} $child.parent".execute()
                }
            }
        }
        String loginShell = "java -jar $jar.absolutePath -login $config.firmAccountName $config.firmAccountPwd"
        def out = new StringBuilder(), err = new StringBuilder()
        executeShell(loginShell, out, err, 5000)
        if (out.length() <= 0 || !out.contains("login success")) {
            println(out.toString())
            println(err.toString())
            println(loginShell)
            throw new GradleException("加固 登录失败")
        }
        return true
    }

    private def clearFirmService() {
        println("加固 清除打包额外配置")
        def clearFirmServiceShell = "java -jar $jar.absolutePath  -config -nocert"
        clearFirmServiceShell.execute()
    }

    static def executeShell(String shellStr,
                            StringBuilder out,
                            StringBuilder err,
                            int millis) {
        def proc = shellStr.execute()
        proc.consumeProcessOutput(out, err)
        proc.waitForOrKill(millis)
    }
}
