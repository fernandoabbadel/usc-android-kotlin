package com.example.usc1.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.ui.theme.UscTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onItemClick: (SettingsItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Central do Sócio",
            subtitle = "Configurações, pedidos e permissões",
            icon = Icons.Outlined.Settings,
        )

        PremiumCard {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = Color.Black,
                    border = BorderStroke(3.dp, PremiumBrand),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_aaakn),
                        contentDescription = "Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = state.userName,
                        color = Color.White,
                        fontSize = 21.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = state.userEmail,
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumChip(label = "Bicho Solto")
                        PremiumChip(label = "Ativo")
                    }
                }
            }
            com.example.usc1.core.ui.PremiumSecondaryButton(
                text = "Abrir Carteirinha",
                onClick = {
                    onItemClick(
                        SettingsItemUiModel(
                            title = "Carteirinha",
                            description = "Identidade digital",
                            action = SettingsAction.Membership,
                        ),
                    )
                },
                icon = Icons.Outlined.CreditCard,
            )
        }

        state.sections.forEach { section ->
            Text(
                text = section.title.uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
            SettingsSection(
                section = section,
                onItemClick = onItemClick,
            )
        }

        Text(
            text = "AAAKN USC • ID: PREVIEW",
            color = PremiumZinc800,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun SettingsScreenPreview() {
    UscTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(),
            onItemClick = {},
        )
    }
}
