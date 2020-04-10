package cn.zengcanxiang.packplugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.GradleException
import org.gradle.api.Project

class PluginExtension {
    static File apkFile
    static File channelOutputFolder
    Boolean isNeedFirm = true
    //输出总路径
    String outPath
    // 加固原文件
    String apkFilePath
    // 原文件mapping.txt
    String mappingPath
    //360加固账号
    String firmAccountName
    //360加固密码
    String firmAccountPwd
    // 渠道文件路径
    File channelConfigFile
    // Android SDK 目录
    File sdkDir
    // 指定的Android build-tools版本
    String buildToolsName
    //apk签名文件路径
    String apkJksPath
    //apk签名文件密码
    String apkJksStorePwd
    //apk签名文件别名
    String apkJksAlias
    //apk签名文件密码
    String apkJksPwd

    static PluginExtension getConfig(Project project) {
        def config = project.getExtensions().findByType(PluginExtension.class)
        if (config == null) {
            throw new GradleException("打包配置为空")
        }
        if (config.outPath == null || config.outPath.length() == 0) {
            config.outPath = project.buildDir
        }
        return config
    }

    def initFirm() {
        if (isNeedFirm && (firmAccountName == null || firmAccountPwd == null)) {
            throw new GradleException("360加固账号密码没有配置")
        }
        if (apkFilePath == null || apkFilePath.length() == 0 || !new File(apkFilePath).exists()) {
            throw new GradleException("apk文件不存在")
        }
    }

    def initSignConfig(Project project) {
        if (this.apkJksPath == null || this.apkJksAlias == null
                || this.apkJksStorePwd == null || this.apkJksPwd == null) {
            BaseExtension extension = project.extensions.getByName("android") as BaseExtension
            Collection<SigningConfig> signingConfigs = extension.getSigningConfigs()
            signingConfigs.forEach { signingConfig ->
                if (signingConfig.name == "release") {
                    this.apkJksPath = signingConfig.storeFile.absolutePath
                    this.apkJksAlias = signingConfig.keyAlias
                    this.apkJksStorePwd = signingConfig.storePassword
                    this.apkJksPwd = signingConfig.keyPassword
                }
            }
            if (this.apkJksPath == null || this.apkJksAlias == null
                    || this.apkJksStorePwd == null || this.apkJksPwd == null) {
                throw new GradleException("签名配置错误(获取项目配置签名失败),至少需要配置签名和360加固账号相关数据\napkJksPath = $apkJksPath, apkJksAlias = $apkJksAlias, apkJksStorePwd = $apkJksStorePwd, apkJksPwd = $apkJksPwd")
            }
        }
    }

    def initSdkDir(Project project) {
        if (sdkDir == null || !sdkDir.exists()) {
            Properties properties = new Properties()
            InputStream inputStream = project.rootProject.file('local.properties').newDataInputStream()
            properties.load(inputStream)
            def sdkDirPath = properties.getProperty('sdk.dir')
            if (sdkDirPath != null && sdkDirPath.length() > 0) {
                sdkDir = new File(sdkDirPath)
            }
            if (!sdkDir.exists()) {
                //去读取环境变量
                properties = System.getProperties()
                sdkDirPath = properties.getProperty("ANDROID_HOME")
                if (sdkDirPath != null && sdkDirPath.length() > 0) {
                    sdkDir = new File(sdkDirPath)
                }
            }
            if (!sdkDir.exists()) {
                throw new GradleException("获取AndroidSDK目录失败(请配置文件或者再local.properties添加sdk_dir或者配置ANDROID_HOME环境变量)")
            }
        }
    }
}
