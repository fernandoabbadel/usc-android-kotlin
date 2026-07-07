package com.example.usc1.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.constants.AppConstants
import com.example.usc1.core.ui.ModuleCard
import com.example.usc1.domain.model.AppModule
import com.example.usc1.domain.model.AppModulePhase
import com.example.usc1.domain.model.AppModules
import com.example.usc1.ui.theme.UscTheme

@Composable
fun DashboardScreen(
    modules: List<AppModule>,
    onOpenModule: (AppModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = AppConstants.AppName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Base Android nativa",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Estrutura inicial para converter o web-reference em Kotlin, Jetpack Compose, Material 3 e Navigation Compose.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ModuleSection(
                title = "Android v1",
                modules = modules.filter { it.phase == AppModulePhase.EssentialV1 },
                onOpenModule = onOpenModule,
            )

            ModuleSection(
                title = "Android v2",
                modules = modules.filter { it.phase == AppModulePhase.ImportantV2 },
                onOpenModule = onOpenModule,
            )
        }
    }
}

@Composable
private fun ModuleSection(
    title: String,
    modules: List<AppModule>,
    onOpenModule: (AppModule) -> Unit,
) {
    if (modules.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        modules.chunked(2).forEach { rowModules ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowModules.forEach { module ->
                    ModuleCard(
                        module = module,
                        onClick = { onOpenModule(module) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowModules.size == 1) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    UscTheme {
        DashboardScreen(
            modules = AppModules.androidModules,
            onOpenModule = {},
        )
    }
}
