package com.autoscript.advanced.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

/**
 * HTTP客户端封装
 * 支持GET、POST等请求
 */
class HttpClient(private val context: Context) {

    /**
     * HTTP请求配置
     */
    data class RequestConfig(
        val connectTimeout: Long = 30000,
        val readTimeout: Long = 30000,
        val writeTimeout: Long = 30000,
        val headers: Map<String, String> = emptyMap(),
        val contentType: String = "application/json",
        val charset: String = "UTF-8",
        val followRedirects: Boolean = true,
        val useCaches: Boolean = false
    )

    /**
     * HTTP响应
     */
    data class HttpResponse(
        val statusCode: Int,
        val headers: Map<String, List<String>>,
        val body: String,
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    /**
     * 请求方法
     */
    enum class Method {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    private var defaultConfig = RequestConfig()

    /**
     * 设置默认配置
     * @param config 默认配置
     */
    fun setDefaultConfig(config: RequestConfig) {
        defaultConfig = config
    }

    /**
     * 发送GET请求
     * @param url 请求URL
     * @param params 请求参数
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun get(
        url: String,
        params: Map<String, String> = emptyMap(),
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(buildUrl(url, params), Method.GET, null, config)
    }

    /**
     * 发送POST请求
     * @param url 请求URL
     * @param body 请求体
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun post(
        url: String,
        body: String? = null,
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(url, Method.POST, body, config)
    }

    /**
     * 发送POST表单请求
     * @param url 请求URL
     * @param formData 表单数据
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        config: RequestConfig = defaultConfig.copy(contentType = "application/x-www-form-urlencoded")
    ): HttpResponse = withContext(Dispatchers.IO) {
        val body = buildFormData(formData)
        executeRequest(url, Method.POST, body, config)
    }

    /**
     * 发送PUT请求
     * @param url 请求URL
     * @param body 请求体
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun put(
        url: String,
        body: String? = null,
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(url, Method.PUT, body, config)
    }

    /**
     * 发送DELETE请求
     * @param url 请求URL
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun delete(
        url: String,
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(url, Method.DELETE, null, config)
    }

    /**
     * 发送PATCH请求
     * @param url 请求URL
     * @param body 请求体
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun patch(
        url: String,
        body: String? = null,
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(url, Method.PATCH, body, config)
    }

    /**
     * 发送HEAD请求
     * @param url 请求URL
     * @param config 请求配置
     * @return HTTP响应
     */
    suspend fun head(
        url: String,
        config: RequestConfig = defaultConfig
    ): HttpResponse = withContext(Dispatchers.IO) {
        executeRequest(url, Method.HEAD, null, config)
    }

    /**
     * 执行HTTP请求
     * @param urlString 请求URL
     * @param method 请求方法
     * @param body 请求体
     * @param config 请求配置
     * @return HTTP响应
     */
    private fun executeRequest(
        urlString: String,
        method: Method,
        body: String?,
        config: RequestConfig
    ): HttpResponse {
        var connection: HttpURLConnection? = null

        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                this.requestMethod = method.name
                this.connectTimeout = config.connectTimeout.toInt()
                this.readTimeout = config.readTimeout.toInt()
                this.instanceFollowRedirects = config.followRedirects
                this.useCaches = config.useCaches

                if (urlString.startsWith("https", ignoreCase = true)) {
                    setupSSL(this as HttpsURLConnection)
                }

                for ((key, value) in config.headers) {
                    setRequestProperty(key, value)
                }

                setRequestProperty("Content-Type", "${config.contentType}; charset=${config.charset}")
                setRequestProperty("Accept-Charset", config.charset)

                if (method != Method.GET && body != null) {
                    doOutput = true
                    DataOutputStream(outputStream).use { out ->
                        out.write(body.toByteArray(charset(config.charset)))
                        out.flush()
                    }
                }
            }

            val statusCode = connection.responseCode
            val responseHeaders = connection.headerFields

            val responseBody = try {
                BufferedReader(InputStreamReader(connection.inputStream, config.charset)).use { reader ->
                    reader.readText()
                }
            } catch (e: Exception) {
                try {
                    BufferedReader(InputStreamReader(connection.errorStream, config.charset)).use { reader ->
                        reader.readText()
                    }
                } catch (e: Exception) {
                    ""
                }
            }

            HttpResponse(
                statusCode = statusCode,
                headers = responseHeaders,
                body = responseBody,
                isSuccess = statusCode in 200..299
            )
        } catch (e: Exception) {
            HttpResponse(
                statusCode = -1,
                headers = emptyMap(),
                body = "",
                isSuccess = false,
                errorMessage = e.message
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 设置SSL
     */
    private fun setupSSL(connection: HttpsURLConnection) {
        try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, null)
            connection.sslSocketFactory = sslContext.socketFactory
            connection.hostnameVerifier = { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 构建带参数的URL
     * @param baseUrl 基础URL
     * @param params 参数
     * @return 完整URL
     */
    private fun buildUrl(baseUrl: String, params: Map<String, String>): String {
        if (params.isEmpty()) return baseUrl

        val separator = if (baseUrl.contains("?")) "&" else "?"
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        return "$baseUrl$separator$queryString"
    }

    /**
     * 构建表单数据
     * @param formData 表单数据
     * @return 表单字符串
     */
    private fun buildFormData(formData: Map<String, String>): String {
        return formData.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }

    /**
     * 同步GET请求
     * @param url 请求URL
     * @param params 请求参数
     * @param config 请求配置
     * @return HTTP响应
     */
    fun getSync(
        url: String,
        params: Map<String, String> = emptyMap(),
        config: RequestConfig = defaultConfig
    ): HttpResponse {
        return executeRequest(buildUrl(url, params), Method.GET, null, config)
    }

    /**
     * 同步POST请求
     * @param url 请求URL
     * @param body 请求体
     * @param config 请求配置
     * @return HTTP响应
     */
    fun postSync(
        url: String,
        body: String? = null,
        config: RequestConfig = defaultConfig
    ): HttpResponse {
        return executeRequest(url, Method.POST, body, config)
    }

    /**
     * 添加请求头
     * @param name 头名称
     * @param value 头值
     * @return 新配置
     */
    fun addHeader(name: String, value: String): RequestConfig {
        val newHeaders = defaultConfig.headers.toMutableMap()
        newHeaders[name] = value
        return defaultConfig.copy(headers = newHeaders)
    }

    /**
     * 设置授权头
     * @param token 授权令牌
     * @return 新配置
     */
    fun setBearerAuth(token: String): RequestConfig {
        return addHeader("Authorization", "Bearer $token")
    }

    /**
     * 设置Basic授权
     * @param username 用户名
     * @param password 密码
     * @return 新配置
     */
    fun setBasicAuth(username: String, password: String): RequestConfig {
        val credentials = android.util.Base64.encodeToString(
            "$username:$password".toByteArray(),
            android.util.Base64.NO_WRAP
        )
        return addHeader("Authorization", "Basic $credentials")
    }
}
