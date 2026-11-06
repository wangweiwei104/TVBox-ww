<div align="center">
  <img src="./app/src/main/res/drawable/app_icon.png" alt="logo" width="100"/>
  <h1 align="center">TV Box</h1>
</div>

<div align="center">更加注重自定义直播功能的app,完全保留原有点播功能！针对新设备优化硬解码方式，如果你的新设备无法硬解码，可以试试</div>

> [!Tip]
> 也是为了练手，以前没学过java,会一点点c,python,matlab，第一次用github,还不怎么会

<br>
<p align="center">
  <a href="https://github.com/wangweiwei104/TVBox-ww/releases/latest">
    <img src="https://img.shields.io/github/v/release/wangweiwei104/TVBox-ww" />
  </a>
  <a href="https://github.com/wangweiwei104/TVBox-ww/releases/latest">
    <img src="https://img.shields.io/github/downloads/wangweiwei104/TVBox-ww/total" />
  </a>
  <a href="https://github.com/wangweiwei104/TVBox-ww/fork">
    <img src="https://img.shields.io/github/forks/wangweiwei104/TVBox-ww" />
  </a>
  <a href="https://github.com/wangweiwei104/TVBox-ww/star">
    <img src="https://img.shields.io/github/stars/wangweiwei104/TVBox-ww" />
  </a>
</p>

- [📍代码修改自(或参考)](#📍代码修改自(或参考))
- [✅ 改了啥](#改了啥)
- [🗓️ 更新日志](./CHANGELOG.md)
- [❤️ 赞赏](#赞赏)
- [📣 免责声明](#免责声明)
- [⚖️ 许可证](#许可证)

## 📍代码修改自(或参考)：

- [takagen99/Box ](https://github.com/takagen99/Box)
- [q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS)

## 改了啥
- ✅ 修改了播放器对硬解码器识别逻辑，比如小米电视 A55 Pro,GPU是G57,解码器是C2开头的，原版无法识别，能识别G52的解码器
- ✅ 修改了设置播放器（解码方式）的逻辑。原逻辑为直接从接口中读取，但如果用户自己定义了直播源，依然使用接口中定义的播放器类型，这就不合适了，造成的表象就是设置中更改了播放器类型，直播中也不生效；现修改为如果用户没有自定义直播源，依然从接口中读取；如果用户自定义了直播源（如http://xx/xx.txt），则从设置中读取；
- ✅ 修改了app图标，使用ChatGPT根据内置banner图生成
- ✅ 修改了内置的epg_data,原有的TV logo地址已失效（我这里测试无法读取），现已改为与q版TVBOX相同的epg_data，TV logo地址可以使用；在用户未自定义logo地址的情况下使用
- ✅ 增加了自定义logo地址的接口，如果用户自定义了logo地址，则不使用内置epg_data中定义的logo地址。自定义logo地址的格式为：
```bash
  http://xx/../{name}.png
  ```
- ✅ 后面陆续改了很多关于直播的功能，具体看release日志吧。。。。。。
- ⭐️ 上面四条更新的图以后再补上吧
- ✨ 代码修改自[takagen99/Box ](https://github.com/takagen99/Box) [20251127-1156](https://github.com/o0HalfLife0o/TVBoxOSC/releases/tag/20251127-1156)
- ✨ 后面再学习一下怎么合并两个仓库的代码，才能保持实时更新

> [!Tip]
> Codec 2.0 (C2)：是 Google 从 Android 9/10 开始引入并强制要求的新一代多媒体框架，用于取代旧的 OMX 架构。
> 命名规范：根据 Android 兼容性定义 (CDD)，所有使用 Codec 2.0 API 的编解码器名称必须以 c2. 开头。
> 原版的IJK 硬解码器识别规则认定只有omx开头的才是硬解码器，导致新设备无法硬解码。
> 另外我发现，25年市场上高端电视采用A73+G52的组合（G52是在安卓9上依然是旧架构），
> 低端电视如A55 pro 的26款改成了5 x A55+G52的组合（25款是 4xA55 + G57）
> 小红书上的视频 http://xhslink.com/o/2jwVqLTJECy

## 使用 VS Code 开发

本项目使用 Gradle Wrapper 构建，不需要单独安装 Gradle。当前构建配置使用 Android Gradle Plugin 7.4.2 和 Gradle 7.5，建议使用 **JDK 11**。

### 安装开发环境

1. 安装 VS Code，并在扩展面板安装工作区推荐的扩展（打开本项目后会显示推荐提示）：Java Extension Pack、Gradle for Java、Kotlin Language 和 Android。
2. 在 VS Code 命令面板运行 `Java: Install New JDK`，选择并安装 JDK 11。安装后重启 VS Code，并确认 `JAVA_HOME` 指向 JDK 11，且 `%JAVA_HOME%\bin` 在 `PATH` 中。
3. 安装 Android SDK。可通过 Android Studio 的 SDK Manager 安装 Android SDK Platform 34 和 Android SDK Platform-Tools（ADB）；设置 `ANDROID_HOME` 指向 SDK 目录。也可在 Android Studio 中打开本项目一次，让它安装所需的 Android SDK 组件。
4. 项目包含 Chaquopy Python 模块。安装 Python 3.8，并确保 `python` 命令可用。若 Python 不在 `PATH` 中，可通过环境变量 `CHAQUOPY_BUILD_PYTHON` 指定 `python.exe` 的完整路径；也可在 Gradle 命令中用 `-PchaquopyBuildPython="C:\路径\python.exe"` 指定。
5. 在 VS Code 中打开仓库根目录，并在弹出的提示中选择安装推荐扩展。

### 构建和安装

- 按 `Ctrl+Shift+B` 执行默认任务 `Android: assembleDebug`，构建所有 Debug 变体。
- 在命令面板运行 `Tasks: Run Task`，可选择 `Android: assembleRelease` 或 `Gradle: clean`。
- 连接已开启 USB 调试的 Android 设备后，运行 `Android: install arm64 debug`，构建并安装 arm64、generic、normal 的 Debug 版本。
- APK 输出在 `app\build\outputs\apk\` 下。

也可以在 VS Code 终端中直接执行：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat installArm64GenericNormalDebug
```

如提示找不到 Android SDK，请确认 `ANDROID_HOME` 设置正确；如提示找不到 Python，请将 Python 3.8 加入 `PATH`，或设置 `CHAQUOPY_BUILD_PYTHON`。

## 更新日志

[更新日志](./CHANGELOG.md)

## 赞赏

<div>暂不需要~</div>


## 关注

不用


## 免责声明

本项目仅供学习交流用途，代码均来源于网络，如有侵权，请联系删除

## 许可证

[MIT](./LICENSE) License &copy; 2025-PRESENT [wangweiwei104](https://github.com/wangweiwei104)
