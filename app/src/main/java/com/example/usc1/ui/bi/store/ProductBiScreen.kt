package com.example.usc1.ui.bi.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.ProductBiDataset
import com.example.usc1.domain.model.ProductBiOption
import com.example.usc1.domain.model.ProductBiScope

/**
 * Tela do BI Loja, nos cinco players (M8.3 + M8.4).
 *
 * Web: o cabeçalho e o `<select>` de produto de
 * `web-reference/src/components/ProductManagementAnalytics.tsx` (545-568), dentro do shell de
 * cada consumidor — `DashboardShell` no tenant, `MiniVendorShell` na lojinha e o painel do
 * coletivo em liga/comissão/diretório.
 *
 * A mesma tela serve os cinco: o que muda é o [ProductBiUiState.dataset], que já chega escopado.
 */
@Composable
fun ProductBiScreen(
    state: ProductBiUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProductSelected: (String) -> Unit = {},
) {
    if (state.isLoading && state.dataset.isEmpty) {
        PremiumLoadingState(text = "Carregando BI da loja", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        ProductBiHeader(
            eyebrow = state.eyebrow,
            title = state.dataset.title,
            subtitle = state.dataset.subtitle,
            onBackClick = onBackClick,
        )

        state.errorMessage?.let { message ->
            PremiumCard(accent = PremiumRed, borderAlpha = 0.45f) {
                Text(text = message, color = PremiumRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // `<select>` de produto (553-567): a opção "todos" vem primeiro, com o rótulo do player.
        PremiumCard(accent = PremiumZinc800, borderAlpha = 0.60f) {
            ProductBiSelect(
                label = "Produto",
                selectedId = state.productFilter,
                allLabel = state.dataset.allLabel,
                options = state.dataset.productOptions,
                onSelect = onProductSelected,
            )
        }

        if (state.isEmptyScope) {
            PremiumEmptyState(
                title = state.emptyTitle,
                subtitle = state.emptySubtitle,
                icon = Icons.Outlined.ExpandMore,
            )
            return@PremiumScreen
        }

        ProductBiView(analytics = state.analytics)
    }
}

@Composable
private fun ProductBiHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBackClick),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Voltar",
                modifier = Modifier.padding(10.dp),
                tint = PremiumZinc400,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProductBiSelect(
    label: String,
    selectedId: String,
    allLabel: String,
    options: List<ProductBiOption>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.title ?: allLabel

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, PremiumZinc800),
                onClick = { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedLabel,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PremiumZinc400,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 340.dp)
                    .background(PremiumZinc900),
            ) {
                DropdownMenuItem(
                    text = { Text(allLabel, color = Color.White, fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onSelect(ProductBiDataset.AllProducts)
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title, color = Color.White, fontSize = 12.sp) },
                        onClick = {
                            expanded = false
                            onSelect(option.id)
                        },
                    )
                }
            }
        }
    }
}

/** Eyebrow de cada player, tirado do shell que envolve o componente no web. */
internal val ProductBiScope.eyebrowLabel: String
    get() = when (this) {
        ProductBiScope.Tenant -> "BI Admin"
        ProductBiScope.League -> "Gestão da liga"
        ProductBiScope.Commission -> "Gestão da comissão"
        ProductBiScope.Directory -> "Gestão do diretório"
        ProductBiScope.MiniVendor -> "Mini vendor"
    }
