package cn.zengcanxiang.packplugin.task

import cn.zengcanxiang.packplugin.PluginExtension
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.TaskAction

class DownloadTask extends DefaultTask {
    static final String down_url_mac = "http://down.360safe.com/360Jiagu/360jiagubao_mac.zip"

    static final String down_url_linux = "http://down.360safe.com/360Jiagu/360jiagubao_linux_64.zip"

    static final String down_url_win = "http://down.360safe.com/360Jiagu/360jiagubao_windows_32.zip"

    String downUrl = down_url_mac

//    private def walle_cli_url = "https://github.com/Meituan-Dianping/walle/releases/download/v1.1.6/walle-cli-all.jar"
    //TODO 由于美团官方暂时没有提供最新的jar。所以下载一个第三方编译的
    private def walle_cli_url = "https://github.com/vclub/vclub.github.io/raw/master/walle-cli-all.jar"

    private File firmZipFile

    private File firmJarParentPath

    private final def firmJarPath = "jiagu/jiagu.jar"

    private PluginExtension config

    DownloadTask() {
        group = "android-pack"
        description = "下载必要的文件(包含360加固和walle-cli.jar)"
        config = PluginExtension.getConfig(project)
    }

    private def initConfig() {
        firmZipFile = new File(config.outPath, "360加固文件压缩包.zip")
        firmJarParentPath = new File(config.outPath, "360")
        def os = System.getProperty("os.name").toLowerCase()
        if (os.contains("linux")) {
            downUrl = down_url_linux
        } else if (os.contains("mac")) {
            downUrl = down_url_mac
        } else {
            downUrl = down_url_win
        }
    }

    @TaskAction
    def download() {
        initConfig()
        if (!isNeedDownload()) {
            println("检测到本地已存在360相关文件")
        } else {
            downLoadFile(downUrl, firmZipFile)
            unZip()
        }
        def walleFile = new File(config.outPath, "walle_cli.jar")
        if (!walleFile.exists()) {
            downLoadFile(walle_cli_url, walleFile)
        }
    }

    private def unZip() {
        println("开始解压文件")
        project.copy(new Action<CopySpec>() {
            @Override
            void execute(CopySpec copySpec) {
                copySpec.from(project.zipTree(firmZipFile))
                        .into(firmJarParentPath)
                println("解压文件结束")
            }
        })
    }

    private def downLoadFile(String downUrl, File saveFile) {
        println("下载文件:$downUrl")
        def connection = new URL(downUrl).openStream()
        def stream2 = new URL(downUrl).openConnection()
        def total = stream2.getContentLength()
        def len
        def hasRead = 0
        byte[] arr = new byte[1024 * 5]
        def out = new FileOutputStream(saveFile)
        def lastResult = 0
        while ((len = connection.read(arr)) != -1) {
            out.write(arr, 0, len)
            hasRead += len
            def decimal = hasRead / total * 100 + ""

            if (decimal != "100")
                decimal = decimal.substring(0, decimal.indexOf("."))

            if (lastResult == Integer.parseInt(decimal)) {
                lastResult++
                println("下载进度：" + decimal + "%")
            }
        }
        connection.close()
        out.close()
        println("下载完成")
    }

    private Boolean isNeedDownload() {
        def firmJar = new File(firmJarParentPath, firmJarPath)
        if (!firmJar.exists()) {
            if (!firmZipFile.exists()) {
                return true
            } else {
                println("检测到本地已存在下载的压缩包")
                unZip()
            }
        }
        return false
    }

    public File getFirmZipFile() {
        return firmZipFile
    }

    public File getFirmJarParentPath() {
        return firmJarParentPath
    }

    public String getDownUrl() {
        return downUrl
    }
}
