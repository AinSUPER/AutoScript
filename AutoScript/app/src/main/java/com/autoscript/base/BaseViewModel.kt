package com.autoscript.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoscript.utils.LogUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LogUtils.e(this::class.simpleName, "Coroutine error", throwable)
        _error.postValue(throwable.message ?: "Unknown error occurred")
        _loading.postValue(false)
    }

    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            block()
        }
    }

    protected fun launchMain(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            block()
        }
    }

    protected fun <T> launchWithResult(
        ioBlock: suspend CoroutineScope.() -> T,
        onSuccess: (T) -> Unit,
        onError: ((Throwable) -> Unit)? = null
    ): Job {
        return viewModelScope.launch(Dispatchers.Main + exceptionHandler) {
            _loading.value = true
            try {
                val result = withContext(Dispatchers.IO) { ioBlock() }
                onSuccess(result)
            } catch (e: Exception) {
                LogUtils.e(this::class.simpleName, "launchWithResult error", e)
                onError?.invoke(e) ?: _error.postValue(e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    protected fun showLoading() {
        _loading.postValue(true)
    }

    protected fun hideLoading() {
        _loading.postValue(false)
    }

    protected fun showError(message: String) {
        _error.postValue(message)
    }

    protected fun showMessage(message: String) {
        _message.postValue(message)
    }

    protected fun clearError() {
        _error.postValue(null)
    }

    protected fun clearMessage() {
        _message.postValue(null)
    }

    override fun onCleared() {
        super.onCleared()
        LogUtils.d(this::class.simpleName, "onCleared")
    }
}
