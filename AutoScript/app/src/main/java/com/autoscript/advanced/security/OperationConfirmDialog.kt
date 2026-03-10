package com.autoscript.advanced.security

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.autoscript.R

/**
 * 操作确认对话框
 * 用于敏感操作前的用户确认
 */
class OperationConfirmDialog(private val context: Context) {

    /**
     * 对话框配置
     */
    data class DialogConfig(
        val title: String = "确认操作",
        val message: String = "",
        val positiveText: String = "确认",
        val negativeText: String = "取消",
        val neutralText: String? = null,
        val showDontAskAgain: Boolean = false,
        val showDetails: Boolean = false,
        val details: String? = null,
        val warningLevel: WarningLevel = WarningLevel.NORMAL,
        val cancellable: Boolean = true,
        val iconResId: Int? = null
    )

    /**
     * 警告级别
     */
    enum class WarningLevel {
        NORMAL, WARNING, DANGER
    }

    /**
     * 对话框结果
     */
    data class DialogResult(
        val confirmed: Boolean,
        val dontAskAgain: Boolean = false,
        val neutralClicked: Boolean = false
    )

    /**
     * 操作类型
     */
    enum class OperationType {
        DELETE_FILE,
        DELETE_SCRIPT,
        RUN_SCRIPT,
        MODIFY_SYSTEM,
        ACCESS_SENSITIVE_DATA,
        SEND_NETWORK_REQUEST,
        MODIFY_SETTINGS,
        UNINSTALL_APP,
        CLEAR_DATA,
        GRANT_PERMISSION,
        EXECUTE_SHELL
    }

    private var dontAskAgainPreference = mutableMapOf<OperationType, Boolean>()

    /**
     * 显示确认对话框
     * @param config 对话框配置
     * @param callback 回调
     */
    fun show(config: DialogConfig, callback: (DialogResult) -> Unit) {
        val builder = AlertDialog.Builder(context)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_operation_confirm, null)

        val titleView = view.findViewById<TextView>(R.id.tv_title)
        val messageView = view.findViewById<TextView>(R.id.tv_message)
        val detailsView = view.findViewById<TextView>(R.id.tv_details)
        val showDetailsBtn = view.findViewById<Button>(R.id.btn_show_details)
        val dontAskAgainCheckBox = view.findViewById<CheckBox>(R.id.cb_dont_ask_again)
        val positiveBtn = view.findViewById<Button>(R.id.btn_positive)
        val negativeBtn = view.findViewById<Button>(R.id.btn_negative)
        val neutralBtn = view.findViewById<Button>(R.id.btn_neutral)

        titleView.text = config.title
        messageView.text = config.message

        if (config.showDetails && config.details != null) {
            showDetailsBtn.visibility = View.VISIBLE
            detailsView.text = config.details

            showDetailsBtn.setOnClickListener {
                if (detailsView.visibility == View.VISIBLE) {
                    detailsView.visibility = View.GONE
                    showDetailsBtn.text = "显示详情"
                } else {
                    detailsView.visibility = View.VISIBLE
                    showDetailsBtn.text = "隐藏详情"
                }
            }
        } else {
            showDetailsBtn.visibility = View.GONE
            detailsView.visibility = View.GONE
        }

        if (config.showDontAskAgain) {
            dontAskAgainCheckBox.visibility = View.VISIBLE
        } else {
            dontAskAgainCheckBox.visibility = View.GONE
        }

        positiveBtn.text = config.positiveText
        negativeBtn.text = config.negativeText

        if (config.neutralText != null) {
            neutralBtn.visibility = View.VISIBLE
            neutralBtn.text = config.neutralText
        } else {
            neutralBtn.visibility = View.GONE
        }

        when (config.warningLevel) {
            WarningLevel.NORMAL -> {
                titleView.setTextColor(android.graphics.Color.BLACK)
            }
            WarningLevel.WARNING -> {
                titleView.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            }
            WarningLevel.DANGER -> {
                titleView.setTextColor(android.graphics.Color.parseColor("#F44336"))
            }
        }

        builder.setView(view)
        builder.setCancelable(config.cancellable)

        val dialog = builder.create()

        positiveBtn.setOnClickListener {
            callback(
                DialogResult(
                    confirmed = true,
                    dontAskAgain = dontAskAgainCheckBox.isChecked
                )
            )
            dialog.dismiss()
        }

        negativeBtn.setOnClickListener {
            callback(DialogResult(confirmed = false))
            dialog.dismiss()
        }

        neutralBtn.setOnClickListener {
            callback(
                DialogResult(
                    confirmed = false,
                    neutralClicked = true
                )
            )
            dialog.dismiss()
        }

        dialog.setOnCancelListener {
            callback(DialogResult(confirmed = false))
        }

        dialog.show()
    }

    /**
     * 显示操作确认对话框
     * @param operationType 操作类型
     * @param operationDetails 操作详情
     * @param callback 回调
     */
    fun confirmOperation(
        operationType: OperationType,
        operationDetails: String = "",
        callback: (DialogResult) -> Unit
    ) {
        if (dontAskAgainPreference[operationType] == true) {
            callback(DialogResult(confirmed = true, dontAskAgain = true))
            return
        }

        val config = getOperationConfig(operationType, operationDetails)
        show(config, callback)
    }

    /**
     * 获取操作配置
     */
    private fun getOperationConfig(operationType: OperationType, details: String): DialogConfig {
        return when (operationType) {
            OperationType.DELETE_FILE -> DialogConfig(
                title = "删除文件",
                message = "确定要删除此文件吗？此操作不可撤销。",
                positiveText = "删除",
                warningLevel = WarningLevel.DANGER,
                showDetails = true,
                details = details
            )

            OperationType.DELETE_SCRIPT -> DialogConfig(
                title = "删除脚本",
                message = "确定要删除此脚本吗？",
                positiveText = "删除",
                warningLevel = WarningLevel.WARNING,
                showDontAskAgain = true,
                showDetails = true,
                details = details
            )

            OperationType.RUN_SCRIPT -> DialogConfig(
                title = "运行脚本",
                message = "确定要运行此脚本吗？",
                positiveText = "运行",
                warningLevel = WarningLevel.NORMAL,
                showDontAskAgain = true,
                showDetails = true,
                details = details
            )

            OperationType.MODIFY_SYSTEM -> DialogConfig(
                title = "修改系统设置",
                message = "此操作将修改系统设置，可能影响设备正常运行。确定要继续吗？",
                positiveText = "继续",
                warningLevel = WarningLevel.DANGER,
                showDetails = true,
                details = details
            )

            OperationType.ACCESS_SENSITIVE_DATA -> DialogConfig(
                title = "访问敏感数据",
                message = "此操作需要访问敏感数据，确定要继续吗？",
                positiveText = "允许",
                warningLevel = WarningLevel.WARNING,
                showDetails = true,
                details = details
            )

            OperationType.SEND_NETWORK_REQUEST -> DialogConfig(
                title = "发送网络请求",
                message = "此操作将发送网络请求，确定要继续吗？",
                positiveText = "发送",
                warningLevel = WarningLevel.NORMAL,
                showDontAskAgain = true,
                showDetails = true,
                details = details
            )

            OperationType.MODIFY_SETTINGS -> DialogConfig(
                title = "修改应用设置",
                message = "确定要修改应用设置吗？",
                positiveText = "修改",
                warningLevel = WarningLevel.NORMAL,
                showDontAskAgain = true
            )

            OperationType.UNINSTALL_APP -> DialogConfig(
                title = "卸载应用",
                message = "确定要卸载此应用吗？",
                positiveText = "卸载",
                warningLevel = WarningLevel.DANGER,
                showDetails = true,
                details = details
            )

            OperationType.CLEAR_DATA -> DialogConfig(
                title = "清除数据",
                message = "确定要清除数据吗？此操作不可撤销。",
                positiveText = "清除",
                warningLevel = WarningLevel.DANGER,
                showDetails = true,
                details = details
            )

            OperationType.GRANT_PERMISSION -> DialogConfig(
                title = "授予权限",
                message = "此操作需要授予权限，确定要继续吗？",
                positiveText = "授权",
                warningLevel = WarningLevel.WARNING,
                showDetails = true,
                details = details
            )

            OperationType.EXECUTE_SHELL -> DialogConfig(
                title = "执行Shell命令",
                message = "此操作将执行Shell命令，可能影响系统稳定性。确定要继续吗？",
                positiveText = "执行",
                warningLevel = WarningLevel.DANGER,
                showDetails = true,
                details = details
            )
        }
    }

    /**
     * 设置"不再询问"偏好
     * @param operationType 操作类型
     * @param dontAskAgain 是否不再询问
     */
    fun setDontAskAgain(operationType: OperationType, dontAskAgain: Boolean) {
        dontAskAgainPreference[operationType] = dontAskAgain
    }

    /**
     * 检查是否设置了"不再询问"
     * @param operationType 操作类型
     * @return 是否设置了不再询问
     */
    fun isDontAskAgain(operationType: OperationType): Boolean {
        return dontAskAgainPreference[operationType] == true
    }

    /**
     * 重置所有"不再询问"设置
     */
    fun resetAllDontAskAgain() {
        dontAskAgainPreference.clear()
    }

    /**
     * 显示简单确认对话框
     * @param title 标题
     * @param message 消息
     * @param callback 回调
     */
    fun showSimpleConfirm(title: String, message: String, callback: (Boolean) -> Unit) {
        show(
            DialogConfig(title = title, message = message),
            { result -> callback(result.confirmed) }
        )
    }

    /**
     * 显示警告对话框
     * @param title 标题
     * @param message 消息
     * @param callback 回调
     */
    fun showWarning(title: String, message: String, callback: (Boolean) -> Unit) {
        show(
            DialogConfig(
                title = title,
                message = message,
                warningLevel = WarningLevel.WARNING
            ),
            { result -> callback(result.confirmed) }
        )
    }

    /**
     * 显示危险操作对话框
     * @param title 标题
     * @param message 消息
     * @param callback 回调
     */
    fun showDanger(title: String, message: String, callback: (Boolean) -> Unit) {
        show(
            DialogConfig(
                title = title,
                message = message,
                warningLevel = WarningLevel.DANGER
            ),
            { result -> callback(result.confirmed) }
        )
    }

    /**
     * 显示带详情的确认对话框
     * @param title 标题
     * @param message 消息
     * @param details 详情
     * @param callback 回调
     */
    fun showWithDetails(title: String, message: String, details: String, callback: (Boolean) -> Unit) {
        show(
            DialogConfig(
                title = title,
                message = message,
                showDetails = true,
                details = details
            ),
            { result -> callback(result.confirmed) }
        )
    }
}
