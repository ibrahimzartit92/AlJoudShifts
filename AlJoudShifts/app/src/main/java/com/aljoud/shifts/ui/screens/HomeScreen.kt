package com.aljoud.shifts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // شعار التطبيق
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 3.dp,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "الجود",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "مرحباً بك في برنامج إدارة الموظفين الخاص بمطعم الجود",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // الصف الأول
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HomeActionButton(
                    title = "الموظفين",
                    icon = Icons.Filled.People,
                    onClick = { onNavigate("employees") },
                    modifier = Modifier.weight(1f)
                )
                HomeActionButton(
                    title = "الشِفتات",
                    icon = Icons.Filled.Schedule,
                    onClick = { onNavigate("shifts") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // الصف الثاني (قوالب الشِفتات + الإعدادات)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HomeActionButton(
                    title = "قوالب الشِفتات",
                    icon = Icons.Filled.ListAlt,
                    onClick = { onNavigate("templates") },
                    modifier = Modifier.weight(1f)
                )

                HomeActionButton(
                    title = "الإعدادات",
                    icon = Icons.Filled.Settings,
                    onClick = { onNavigate("settings") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ✅ زر الفروع — مخفي مؤقتًا (بس شيل التعليق من السطرين ليرجع)
            /*
            HomeActionButton(
                title = "الفروع",
                icon = Icons.Filled.Store,
                onClick = { onNavigate("branches") },
                modifier = Modifier.fillMaxWidth()
            )
            */

            Spacer(Modifier.height(24.dp))

            Text(
                "جميع الحقوق محفوظة Ibrahim Zartit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(0.7f)
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
    }
}
