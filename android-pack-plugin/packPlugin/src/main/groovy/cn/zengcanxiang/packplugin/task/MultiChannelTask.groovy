package cn.zengcanxiang.packplugin.task

import cn.zengcanxiang.packplugin.PluginExtension
import com.android.apksigner.core.ApkVerifier
import com.android.apksigner.core.internal.util.ByteBufferDataSource
import com.android.apksigner.core.util.DataSource
import com.android.build.gradle.BaseExtension
import groovy.io.FileType
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class MultiChannelTask extends DefaultTask {

    File zipAlignFile

    File signFile

    PluginExtension config

    BaseExtension extension

    MultiChannelTask() {
        group = "android-pack"
        description = "注入多渠道"
        config = PluginExtension.getConfig(project)
    }

    @TaskAction
    def multiChannel() {
        println("多渠道注入任务开始")
        if (config.channelConfigFile == null || !config.channelConfigFile.exists()) {
            println("多渠道打包配置文件不存在,将不执行渠道注入任务")
            return
        }

        def androidExtensions = project.extensions.getByName("android")
        if( androidExtensions != null instanceof BaseExtension){
            extension = androidExtensions as BaseExtension
        }else{
            println("当前不在android工程内。无法获取项目版本相关信息和android签名工具路径")
            return
        }

        def apkFile = PluginExtension.apkFile
        if (apkFile == null) {
            apkFile = new File(config.apkFilePath)
        }
        if (apkFile == null || !apkFile.exists()) {
            throw new GradleException("多渠道原apk：${apkFile}， is not existed!")
        }

        Map<String, String> nameVariantMap = [
                'appName'      : project.name,
                'projectName'  : project.rootProject.name,
                'applicationId': extension.defaultConfig.applicationId,
                'versionName'  : extension.defaultConfig.versionName,
                'versionCode'  : extension.defaultConfig.versionCode.toString()
        ]
        println("对apk进行签名")
        def signApkPath = generateApkSinger(apkFile)
        if (config.channelConfigFile != null && config.channelConfigFile.exists()) {
            println("开始注入多渠道")
            File channelOutputFolderParent = new File(
                    new File(config.outPath, "channelResult"),
                    nameVariantMap["applicationId"]
            )
            channelOutputFolderParent.mkdirs()
            File channelOutputFolder = new File(
                    channelOutputFolderParent, new Date().format("yyyy-MM-dd-HH-mm-s")
            )
            channelOutputFolder.mkdirs()
            PluginExtension.channelOutputFolder = channelOutputFolder
            generateChannelApkByConfigFile(config.channelConfigFile,
                    signApkPath,
                    channelOutputFolder,
                    nameVariantMap
            )
        }
    }

    private def generateChannelApkByConfigFile(File configFile,
                                               String apkFile,
                                               File channelOutputFolder,
                                               Map<String, String> nameVariantMap
    ) {
        def walleJarFile = new File(config.outPath, "walle_cli.jar")
        if (!walleJarFile.exists()) {
            println("请下载walle_cli.jar文件")
            return
        }
        def writeChannelShell = "java -jar $walleJarFile.absolutePath  batch2 -f $configFile.absolutePath $apkFile $channelOutputFolder.absolutePath"
        def out = new StringBuilder(), err = new StringBuilder()
        println("注入渠道命令为：$writeChannelShell")
        FirmTask.executeShell(writeChannelShell, out, err, 1000 * 60 * 10)
        new CopySourceTask(project).copySource()
    }

    String generateApkSinger(File apkFile) {
        def apkPath = apkFile.absolutePath

        getBuildPath(extension.buildToolsVersion)
        config.initSignConfig(project)
        String zip_aligned_apk_path = apkPath.substring(0, apkPath.length() - 4) + "_zip.apk"
        String signed_apk_path = zip_aligned_apk_path.substring(0, zip_aligned_apk_path.length() - 4) + "_signer.apk"
        def out = new StringBuilder(), err = new StringBuilder()
        // APK zip对齐命令 xxx/zipalign -v 4 xx.apk xx_aligned.apk
        def zipAlignShell = "$zipAlignFile.absolutePath -v 4 $apkPath $zip_aligned_apk_path"
        //APK 签名命令
        def signedShell = "$signFile.absolutePath sign --ks $config.apkJksPath --ks-key-alias $config.apkJksAlias --ks-pass pass:$config.apkJksStorePwd --key-pass pass:$config.apkJksPwd --out $signed_apk_path  $zip_aligned_apk_path"
        println("对齐命令为：$zipAlignShell")
        FirmTask.executeShell(zipAlignShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("对齐错误:$zipAlignShell")
            println(err.toString())
            throw new GradleException(err.toString())
        }
        println("签名命令为：$signedShell")
        FirmTask.executeShell(signedShell, out, err, 1000 * 60 * 10)
        if (err != null && err.length() > 0) {
            println("签名错误:$signedShell")
            println(err.toString())
            throw new GradleException(err.toString())
        }
        checkV2Signature(project.file(signed_apk_path))
        return signed_apk_path
    }

    def getBuildPath(String buildVersion) {
        config.initSdkDir(project)
        def buildToolParent = new File(config.sdkDir, "build-tools")
        File apkBuild
        if (config.buildToolsName != null && config.buildToolsName.length() > 0) {
            apkBuild = new File(buildToolParent, config.buildToolsName)
        } else {
            apkBuild = new File(buildToolParent, buildVersion)
        }
        println("获取的sdk build-tools目录为：$apkBuild.absolutePath")
        if (apkBuild.exists()) {
            apkBuild.eachFile { childFile ->
                if (childFile.name.contains("zipalign")) {
                    zipAlignFile = childFile
                }
                if (childFile.name.contains("apksigner")) {
                    signFile = childFile
                }
            }
        }
        // 如果这两个有一个为空 则去遍历android_home/build-tools/目录
        if (zipAlignFile == null || signFile == null) {
            buildToolParent.eachFileRecurse(FileType.DIRECTORIES) { dir ->
                dir.eachFile { childFile ->
                    if (childFile.name.contains("zipalign")) {
                        zipAlignFile = childFile
                    }
                    if (childFile.name.contains("apksigner")) {
                        signFile = childFile
                    }
                }
            }
            if (zipAlignFile == null || signFile == null) {
                throw new GradleException("无法找到build_tools工具，请下载最新的build_tools工具")
            }
        }
    }

    private static def checkV2Signature(File apkFile) {
        println("检查apk v2签名空间")
        FileInputStream fIn = null
        FileChannel fChan = null
        try {
            fIn = new FileInputStream(apkFile)
            fChan = fIn.getChannel()
            long fSize = fChan.size()
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fSize)
            fChan.read(byteBuffer)
            byteBuffer.rewind()
            DataSource dataSource = new ByteBufferDataSource(byteBuffer)
            ApkVerifier apkVerifier = new ApkVerifier()
            ApkVerifier.Result result = apkVerifier.verify(dataSource, 0)
            if (!result.verified || !result.verifiedUsingV2Scheme) {
                throw new GradleException("${apkFile} has no v2 signature in Apk Signing Block!")
            }
        } catch (IOException ignore) {
            ignore.printStackTrace()
        } finally {
            IOUtils.closeQuietly(fChan)
            IOUtils.closeQuietly(fIn)
        }
    }
}
