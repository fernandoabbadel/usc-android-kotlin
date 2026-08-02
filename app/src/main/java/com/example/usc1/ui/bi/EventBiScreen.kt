package com.example.usc1.ui.bi

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrackChanges
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.EventBiAudienceBasis
import com.example.usc1.domain.model.EventBiOption
import com.example.usc1.domain.model.EventBiScopeRef
import com.example.usc1.domain.model.EventBiView
import com.example.usc1.domain.model.headerFor
import com.example.usc1.ui.bi.views.EventBiCommercialView
import com.example.usc1.ui.bi.views.EventBiGateView
import com.example.usc1.ui.bi.views.EventBiOperationalView
import com.example.usc1.ui.bi.views.EventBiSalesView
import com.example.usc1.ui.bi.views.EventBiStrategicView

/**
 * BI de Eventos — shell, cabeçalho de contexto e hub (M8.1).
 *
 * Web: `DashboardShell`, `Filters` e `HubContent` de
 * `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`.
 *
 * A mesma tela serve os quatro players; o que muda é o `EventBiContext`.
 */
@Composable
fun EventBiScreen(
    state: EventBiUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEventSelected: (String) -> Unit = {},
    onProductSelected: (String) -> Unit = {},
    onStartDateChange: (String) -> Unit = {},
    onEndDateChange: (String) -> Unit = {},
    onAudienceBasisChange: (EventBiAudienceBasis) -> Unit = {},
    onModuleClick: (EventBiView) -> Unit = {},
) {
    val header = state.context.headerFor(state.view)

    // Primeira carga do escopo; as trocas de filtro seguintes mantêm a tela montada.
    if (state.isLoading && state.dataset.isEmpty) {
        PremiumLoadingState(text = "Carregando BI de eventos", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        EventBiHeader(
            eyebrow = header.eyebrow,
            title = header.title,
            subtitle = header.subtitle,
            logoUrl = state.context.contextLogo,
            onBackClick = onBackClick,
        )

        state.errorMessage?.let { EventBiBanner(text = it, accent = PremiumRed) }

        // Banner "Este evento pertence a outro portal" (web 6670-6681). O web faz
        // `router.replace`; aqui o destino é o workspace de evento, que é o M10, então o
        // aviso aparece e a navegação fica para lá.
        state.ownerRedirect?.let { redirect ->
            EventBiBanner(text = redirect.message, accent = PremiumAmber, textColor = PremiumAmber)
        }

        EventBiFilters(
            state = state,
            onEventSelected = onEventSelected,
            onProductSelected = onProductSelected,
            onStartDateChange = onStartDateChange,
            onEndDateChange = onEndDateChange,
            onAudienceBasisChange = onAudienceBasisChange,
        )

        when (state.view) {
            EventBiView.Home -> EventBiHubContent(onModuleClick = onModuleClick)

            EventBiView.Commercial -> EventBiCommercialView(
                analytics = state.analytics,
                audienceBasis = state.filter.audienceBasis,
                onAudienceBasisChange = onAudienceBasisChange,
            )

            EventBiView.Operational -> EventBiOperationalView(analytics = state.analytics)

            EventBiView.Gate -> EventBiGateView(analytics = state.analytics)

            EventBiView.Strategic -> EventBiStrategicView(analytics = state.analytics)

            EventBiView.Sales -> EventBiSalesView(
                analytics = state.analytics,
                withdrawalLegendLinks = state.withdrawalLegendLinks,
            )
        }
    }
}

/** Cabeçalho do `DashboardShell`: voltar, logo, eyebrow, título e subtítulo. */
@Composable
private fun EventBiHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    logoUrl: String,
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

        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
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

/**
 * `HubContent` do web: os cinco módulos com título e subtítulo exatos do array `MODULES`.
 *
 * No M8.1 os cards ainda não navegam — as visões são o M8.2.
 */
@Composable
private fun EventBiHubContent(
    onModuleClick: (EventBiView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EventBiView.modules.forEach { module ->
            EventBiModuleCard(module = module, onClick = { onModuleClick(module) })
        }
    }
}

@Composable
private fun EventBiModuleCard(
    module: EventBiView,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = PremiumBrand,
            )
            Text(
                text = module.title.uppercase(),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = module.subtitle,
                color = PremiumZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Ícones do array `MODULES` do web. */
private val EventBiView.icon: ImageVector
    get() = when (this) {
        EventBiView.Home -> Icons.Outlined.TrackChanges
        EventBiView.Commercial -> Icons.Outlined.AttachMoney
        EventBiView.Operational -> Icons.Outlined.AccessTime
        EventBiView.Gate -> Icons.Outlined.QrCode2
        EventBiView.Strategic -> Icons.Outlined.TrackChanges
        EventBiView.Sales -> Icons.Outlined.ShoppingBag
    }

/**
 * `Filters` do web.
 *
 * Diferença registrada: no web o bloco de filtros só aparece nas cinco visões — o hub
 * (`view === "inicio"`) renderiza apenas os cards. No app ele já aparece no hub porque o
 * estado de filtro é compartilhado com as visões do M8.2 e é o que prova o escopo da consulta.
 */
@Composable
private fun EventBiFilters(
    state: EventBiUiState,
    onEventSelected: (String) -> Unit,
    onProductSelected: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onAudienceBasisChange: (EventBiAudienceBasis) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCard(modifier = modifier, accent = PremiumZinc800, borderAlpha = 0.60f) {
        if (state.scopeLocked) {
            // Bloco travado do `Filters`: `scopeLabel || (scopeType === "tenant" ? "Atlética" : "Entidade")`.
            EventBiLockedScope(label = state.context.lockedScopeLabel)
        }

        EventBiSelect(
            label = "Evento",
            selectedId = state.filter.eventId,
            allLabel = "Todos os eventos",
            options = state.eventOptions,
            onSelect = onEventSelected,
        )

        if (state.showsProductFilter) {
            EventBiSelect(
                label = "Produto",
                selectedId = state.filter.productId,
                allLabel = "Todos os produtos",
                options = state.productOptions,
                onSelect = onProductSelected,
            )
        }

        PremiumTextField(
            value = state.filter.startDate,
            onValueChange = onStartDateChange,
            label = "Início (AAAA-MM-DD)",
        )
        PremiumTextField(
            value = state.filter.endDate,
            onValueChange = onEndDateChange,
            label = "Fim (AAAA-MM-DD)",
        )

        // `AUDIENCE_BASIS_OPTIONS`: base do público usada pelas visões.
        Text(
            text = "Base do público".uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventBiAudienceBasis.entries.forEach { basis ->
                EventBiToggle(
                    label = basis.label,
                    isActive = state.filter.audienceBasis == basis,
                    onClick = { onAudienceBasisChange(basis) },
                )
            }
        }
    }
}

@Composable
private fun EventBiLockedScope(label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            color = PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

/** `<select>` do `Filters`, com a opção "todos" na frente da lista. */
@Composable
private fun EventBiSelect(
    label: String,
    selectedId: String,
    allLabel: String,
    options: List<EventBiOption>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selectedId }?.name ?: allLabel

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedLabel,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = PremiumZinc400,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(text = allLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    onClick = {
                        expanded = false
                        onSelect(EventBiScopeRef.All)
                    },
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
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

@Composable
private fun EventBiToggle(label: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (isActive) PremiumBrand.copy(alpha = 0.16f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (isActive) PremiumBrand.copy(alpha = 0.42f) else PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = if (isActive) PremiumBrand else PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun EventBiBanner(text: String, accent: Color, textColor: Color = accent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
