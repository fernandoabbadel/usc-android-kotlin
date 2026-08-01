package com.example.usc1.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.data.repository.SupabaseEventsRepository
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.repository.EventsRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SalesModeEventsViewModel(
    private val eventsRepository: EventsRepository = SupabaseEventsRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(EventSalesModeUiState(isLoading = true))
    val uiState: StateFlow<EventSalesModeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val feedEvents = eventsRepository.getEvents()
                    .filter { event -> event.isEventMenuEnabled || event.menuProducts.isNotEmpty() }
                val detailedEvents = feedEvents.map { event ->
                    runCatching { eventsRepository.getEventById(event.id) }
                        .getOrNull()
                        ?: event
                }
                    .filter { event -> event.isEventMenuEnabled || event.menuProducts.isNotEmpty() }

                _uiState.value = EventSalesModeUiState(
                    isLoading = false,
                    events = detailedEvents,
                )
            } catch (error: Throwable) {
                _uiState.value = EventSalesModeUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o Modo Vendas.",
                )
            }
        }
    }
}

data class EventSalesModeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val events: List<Event> = emptyList(),
) {
    val products: List<EventSalesModeProduct>
        get() = events.flatMap { event ->
            event.menuProducts.map { product ->
                EventSalesModeProduct(event = event, product = product)
            }
        }

    val activeEvents: List<Event>
        get() = events.filter { event -> event.status == EventStatus.Open || event.menuProducts.isNotEmpty() }

    val totalStock: Int
        get() = products.sumOf { item -> item.product.stockCount.coerceAtLeast(0) }

    val totalPotentialRevenue: Double
        get() = products.sumOf { item -> item.product.priceValue * item.product.stockCount.coerceAtLeast(0) }
}

data class EventSalesModeProduct(
    val event: Event,
    val product: EventMenuProduct,
)

@Composable
fun EventSalesModeScreen(
    state: EventSalesModeUiState,
    onEventClick: (String) -> Unit,
    onEventMenuClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScannerClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando modo vendas", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = "Modo Vendas",
                subtitle = "Operação de evento",
                icon = Icons.Outlined.Storefront,
            )
            PremiumEmptyState(
                title = "Modo vendas indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Storefront,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        else -> EventSalesModeLoadedContent(
            state = state,
            onEventClick = onEventClick,
            onEventMenuClick = onEventMenuClick,
            onOrdersClick = onOrdersClick,
            onScannerClick = onScannerClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventSalesModeLoadedContent(
    state: EventSalesModeUiState,
    onEventClick: (String) -> Unit,
    onEventMenuClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = "Modo Vendas",
            subtitle = "Produtos, fichas e retiradas por evento",
            icon = Icons.Outlined.Storefront,
        )

        PremiumCard(accent = PremiumAmber) {
            PremiumChip(
                label = "OPERAÇÃO DO EVENTO",
                icon = Icons.Outlined.EventAvailable,
                accent = PremiumAmber,
                filled = true,
            )
            Text(
                text = "CATÁLOGO DE VENDAS",
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = "Aqui entram apenas produtos vinculados ao cardápio do evento. Ingressos continuam nos lotes.",
                color = PremiumZinc300,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryButton(
                    text = "Menu",
                    onClick = onEventMenuClick,
                    icon = Icons.Outlined.Inventory2,
                    modifier = Modifier.weight(1f),
                    accent = PremiumAmber,
                )
                PremiumSecondaryButton(
                    text = "Scanner",
                    onClick = onScannerClick,
                    icon = Icons.Outlined.QrCodeScanner,
                    modifier = Modifier.weight(1f),
                    accent = PremiumBrand,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EventSalesModeMetric(
                label = "Eventos",
                value = state.activeEvents.size.toString(),
                icon = Icons.Outlined.EventAvailable,
                modifier = Modifier.weight(1f),
            )
            EventSalesModeMetric(
                label = "Produtos",
                value = state.products.size.toString(),
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EventSalesModeMetric(
                label = "Estoque",
                value = state.totalStock.toString(),
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f),
            )
            EventSalesModeMetric(
                label = "Potencial",
                value = formatter.format(state.totalPotentialRevenue),
                icon = Icons.Outlined.AccountBalanceWallet,
                modifier = Modifier.weight(1f),
            )
        }

        PremiumSecondaryButton(
            text = "Pedidos do evento",
            onClick = onOrdersClick,
            icon = Icons.Outlined.ReceiptLong,
            accent = PremiumBrand,
        )

        if (state.events.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum cardápio ativo",
                subtitle = "Quando um evento tiver `eventParty` ativo, ele aparecerá aqui com os produtos do modo vendas.",
                icon = Icons.Outlined.Storefront,
            )
        } else {
            PremiumHeader(
                title = "Eventos em operação",
                subtitle = "Toque para abrir o detalhe e registrar pedido",
                icon = Icons.Outlined.EventAvailable,
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.events.forEach { event ->
                    EventSalesModeEventCard(
                        event = event,
                        onClick = { onEventClick(event.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun EventSalesModeMenuScreen(
    state: EventSalesModeUiState,
    onProductClick: (String) -> Unit,
    onOrdersClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando menu do evento", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = "Menu do Evento",
                subtitle = "Produtos do modo vendas",
                icon = Icons.Outlined.Inventory2,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Menu indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Inventory2,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        else -> EventSalesModeMenuLoadedContent(
            state = state,
            onProductClick = onProductClick,
            onOrdersClick = onOrdersClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventSalesModeMenuLoadedContent(
    state: EventSalesModeUiState,
    onProductClick: (String) -> Unit,
    onOrdersClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Todos") }
    val categories = listOf("Todos") + state.products
        .map { item -> item.product.category.ifBlank { "Geral" } }
        .distinct()
        .sorted()
    val visibleProducts = state.products.filter { item ->
        val query = searchQuery.trim()
        val categoryMatches = selectedCategory == "Todos" ||
            item.product.category.equals(selectedCategory, ignoreCase = true)
        val searchMatches = query.isBlank() ||
            item.product.name.contains(query, ignoreCase = true) ||
            item.product.description.contains(query, ignoreCase = true) ||
            item.event.title.contains(query, ignoreCase = true) ||
            item.product.category.contains(query, ignoreCase = true)
        categoryMatches && searchMatches
    }

    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = "Menu do Evento",
            subtitle = "Produtos disponíveis no modo vendas",
            icon = Icons.Outlined.Inventory2,
            onBackClick = onBackClick,
        )

        PremiumTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Buscar produto, evento ou categoria",
            leadingIcon = Icons.Outlined.Search,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                Box(modifier = Modifier.clickable { selectedCategory = category }) {
                    PremiumChip(
                        label = category,
                        icon = if (category == "Todos") Icons.Outlined.Tune else null,
                        accent = if (selectedCategory == category) PremiumAmber else PremiumZinc500,
                        filled = selectedCategory == category,
                    )
                }
            }
        }

        PremiumSecondaryButton(
            text = "Acompanhar pedidos",
            onClick = onOrdersClick,
            icon = Icons.Outlined.ReceiptLong,
            accent = PremiumBrand,
        )

        if (visibleProducts.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum produto encontrado",
                subtitle = "Ajuste a busca ou aguarde produtos vinculados ao cardápio do evento.",
                icon = Icons.Outlined.Inventory2,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleProducts.forEach { item ->
                    EventSalesModeProductCard(
                        item = item,
                        onClick = { onProductClick(item.event.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSalesModeMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    PremiumCard(modifier = modifier, accent = PremiumBrand) {
        Icon(icon, contentDescription = null, tint = PremiumBrand, modifier = Modifier.size(22.dp))
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EventSalesModeEventCard(
    event: Event,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PremiumZinc900)
            .border(BorderStroke(1.dp, PremiumAmber.copy(alpha = 0.48f)), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        EventCover(
            event = event,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumChip(label = "MODO VENDAS", icon = Icons.Outlined.Storefront, accent = PremiumAmber, filled = true)
                PremiumChip(label = "${event.menuProducts.size} produtos", icon = Icons.Outlined.Inventory2, accent = PremiumBrand)
            }
            Text(
                text = event.eventMenuTitle.ifBlank { "Menu do evento" },
                color = PremiumAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 23.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            EventMetaLine(event = event)
            PremiumInfoRow(
                label = "Operação",
                value = "${event.eventMenuCategory} • ${event.ownerType.label}",
                accent = PremiumAmber,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (event.menuProducts.isEmpty()) {
                        "Produtos ainda não carregados"
                    } else {
                        "Abrir cardápio e pedidos"
                    },
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp),
                        tint = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventSalesModeProductCard(
    item: EventSalesModeProduct,
    onClick: () -> Unit,
) {
    val product = item.product
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumZinc900)
            .border(BorderStroke(1.dp, PremiumAmber.copy(alpha = 0.38f)), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PremiumAmber)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumChip(label = product.category.ifBlank { "Menu" }, accent = PremiumAmber, filled = true)
                PremiumChip(label = item.event.title.take(18), accent = PremiumBrand)
            }
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.description.ifBlank { item.event.eventMenuCategory },
                color = PremiumZinc400,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = product.priceLabel,
                color = PremiumAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = product.stockLabel,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Payments, contentDescription = null, tint = PremiumBrand, modifier = Modifier.size(13.dp))
                Text(
                    text = "pedido",
                    color = PremiumBrand,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}
