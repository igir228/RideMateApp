package com.kaory.ridemate.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaory.ridemate.domain.notification.NotificationEngine
import com.kaory.ridemate.domain.notification.model.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    val engine: NotificationEngine
) : ViewModel() {

    /** Теперь показываем ВСЕ уведомления */
    val notifications: StateFlow<List<Notification>> = engine.allNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun dismiss(id: String) {
        engine.dismissNotification(id)
    }
}