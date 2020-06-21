

# Android-pack-plugin

Android的打包gradle插件。

最少只需要配置加固账号即刻享受加固、签名、注入渠道一条龙服务。

优点：

- 免配置繁琐的加固文件路径：通过获取系统环境，下载对应360加固文件。
- 免配置繁琐的签名信息：通过获取项目配置，自动获取版本信息和签名信息。
- 可通过配置，导出原Apk和mapping文件，项目build产物可以在同一路径下可见。		

## 使用步骤（可参考sample）

1. 项目build.gradle下添加 

  ```groovy
    repositories{
    	jcenter()
    }
    dependencies{
    	classpath 'cn.zengcanxiang:android-pack-plugin:1.0.1'
    }
  ```

   

2. sync同步之后，app项目apply插件。

  ```groovy
  apply plugin: 'cn.zengcanxiang.androidPackPlugin'
  
  androidPackPlugin {
   		apkFilePath "${project.projectDir}/app-release.apk"
   		// 360加固账号用户名，建议配置到本地文件中。
   		firmAccountName "firmAccountName"
   		// 360加固账号密码，建议配置到本地文件中。
   		firmAccountPwd "firmAccountPwd"
   		// waller渠道包的渠道配置文件。具体格式可以参考sample下的config文件
   		channelConfigFile project.file("${project.projectDir}/channleConfig.json")
   		// 360加固所需要文件下载地址和加固、渠道文件输出目录，建议配置到本地文件中。
   		// 比较好兼容团队中mac电脑和Windows电脑不同环境里的尴尬
   		// 默认为项目的build目录
   		outPath "outPath"
  }
  ```

3. 在gradle面板里通过task列表里，android-pack分组里的task来进行操作。

   - downTask 判断当前所在的系统，进行360加固相关文件的下载。如果本地以及存在相关压缩包，则进行解压，减少重复下载。
- firmTask 依赖于downTask。 登录360账号，清除config设置(个人不喜欢添加加固方的功能)，然后下载到指定位置下的firmResult目录。
   - MultiChannelTask 依赖于firmTask。获取Android项目里build.gradle配置的相关信息，通过这些对加固后的apk进行签名和渠道注入。

## TODO

- [ ] 360config高级设置支持(主要是没有高贵的vip体验，无法验证效果是否有效)
- [ ] 持续跟进waller的最新版本
- [ ] 多渠道打包在高版本api好像存在兼容问题 