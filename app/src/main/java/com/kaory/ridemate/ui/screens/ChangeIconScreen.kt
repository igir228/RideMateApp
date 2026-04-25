package com.kaory.ridemate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Расширенный набор иконок для уведомлений (эмодзи)
val iconOptions = listOf(
    // Скорость / движение
    "🚀", "⚡", "💨", "🏎️", "🔥", "🎯", "🚴", "🛴",
    // Расстояние / пробег
    "📍", "🗺️", "🧭", "🛣️", "🛤️", "🏁", "🎌", "📏",
    // Велосипед / транспорт
    "🚲", "🚲‍♂️", "🚵", "🚵‍♂️", "🛵", "🏍️", "🚗", "🚌",
    // Сервис / обслуживание
    "🔧", "🔩", "⚙️", "🔋", "🪫", "🛢️", "🧰", "🛠️",
    // Предупреждения / Ошибки
    "⚠️", "🚨", "⛔", "❗", "❕", "✅", "❌", "💡",
    // Достижения / Праздники
    "🏆", "🎉", "🎊", "🥇", "⭐", "🌟", "💎", "🔔",
    // Погода / Окружающая среда
    "☀️", "🌧️", "❄️", "🌙", "💧", "🌡️", "🧊", "🌪️",
    // Здоровье / Личное
    "💪", "❤️", "🧠", "🫁", "🍎", "💊", "🧘", "😎"
)

@Composable
fun ChangeIconScreen(
    currentIcon: String,
    onIconSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Icon",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),   // 6 иконок в ряд для удобства выбора
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(iconOptions) { icon ->
                val isSelected = icon == currentIcon
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onIconSelected(icon) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = icon,
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}