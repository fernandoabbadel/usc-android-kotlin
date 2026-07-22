package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminMiniVendorsHubScreen(
    onApprovalsClick: () -> Unit,
    onVendorsClick: () -> Unit,
    onExitAdminClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Mini Vendor Admin",
            subtitle = "Abra cada área em página própria para aprovações e cadastros.",
            icon = Icons.Outlined.Store,
            accent = Color(0xFF60A5FA),
            onBackClick = onBackClick,
        )

        PremiumSecondaryButton(
            text = "Sair do admin",
            onClick = onExitAdminClick,
            accent = PremiumBrandAccent,
            icon = Icons.Outlined.Dashboard,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MiniVendorHubCard(
                title = "Pendentes de aprovação",
                description = "Abra a fila separada para aprovar ou rejeitar novos cadastros sem misturar com a listagem geral.",
                buttonLabel = "Abrir aprovações",
                icon = Icons.Outlined.Shield,
                accent = PremiumAmber,
                onClick = onApprovalsClick,
                modifier = Modifier.fillMaxWidth(),
            )
            MiniVendorHubCard(
                title = "Todos os mini vendors",
                description = "Veja todas as lojinhas cadastradas em uma página própria, com status e ações de administração.",
                buttonLabel = "Abrir cadastros",
                icon = Icons.Outlined.Store,
                accent = Color(0xFF60A5FA),
                onClick = onVendorsClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MiniVendorHubCard(
    title: String,
    description: String,
    buttonLabel: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = description,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.30f),
                border = BorderStroke(1.dp, PremiumZinc800),
                onClick = onClick,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buttonLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = PremiumZinc500,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
