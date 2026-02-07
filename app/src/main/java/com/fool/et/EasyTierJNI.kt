package com.fool.et.jni

/** EasyTier JNI 接口类 提供 Android 应用调用 EasyTier 网络功能的接口 */
object EasyTierJNI {
    init {
        System.loadLibrary("easytier_android_jni")
    }

    /**
     * 设置 TUN 文件描述符
     * @param instanceName 实例名称
     * @param fd TUN 文件描述符
     * @return 0 表示成功，-1 表示失败
     */
    @JvmStatic external fun setTunFd(instanceName: String, fd: Int): Int

    /**
     * 解析配置字符串 (暂未使用，runNetworkInstance 内部可能已包含)
     */
    @JvmStatic external fun parseConfig(config: String): Int

    /**
     * 运行网络实例 (核心启动函数)
     * @param config TOML 格式的配置字符串
     * @return 0 表示成功，-1 表示失败
     */
    @JvmStatic external fun runNetworkInstance(config: String): Int

    /**
     * 保留指定的网络实例，停止其他实例
     * @param instanceNames 要保留的实例名称数组，传入 null 将停止所有实例
     */
    @JvmStatic external fun retainNetworkInstance(instanceNames: Array<String>?): Int

    /**
     * 收集网络信息 (调试用)
     */
    @JvmStatic external fun collectNetworkInfos(maxLength: Int): String?

    /**
     * 获取最后的错误消息
     */
    @JvmStatic external fun getLastError(): String?

    // --- 便利方法 ---
    @JvmStatic fun stopAllInstances(): Int = retainNetworkInstance(null)
}
