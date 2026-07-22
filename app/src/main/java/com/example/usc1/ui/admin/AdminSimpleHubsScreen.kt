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
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WorkspacePremium
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
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

data class AdminHubItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color = PremiumBrand,
    val onClick: () -> Unit,
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminPlansHubScreen(
    onHistoryClick: () -> Unit,
    onAuditClick: () -> Unit,
    onEditCatalogClick: () -> Unit,
    onBichoSoltoClick: () -> Unit,
    onCardumeLivreClick: () -> Unit,
    onAtletaClick: () -> Unit,
    onLendaClick: () -> Unit,
    onPublicPlansClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val planListItems = listOf(
        AdminHubItem("Lista Bicho Solto", "Assinaturas por plano", Icons.Outlined.Groups, onClick = onBichoSoltoClick),
        AdminHubItem("Lista Cardume Livre", "Assinaturas por plano", Icons.Outlined.Groups, onClick = onCardumeLivreClick),
        AdminHubItem("Lista Atleta", "Assinaturas por plano", Icons.Outlined.Groups, onClick = onAtletaClick),
        AdminHubItem("Lista Lenda", "Assinaturas por plano", Icons.Outlined.Groups, onClick = onLendaClick),
    )
    val managementItems = listOf(
        AdminHubItem("Histórico", "Solicitações e aprovações", Icons.Outlined.History, onClick = onHistoryClick),
        AdminHubItem("Auditoria", "Conferência de status", Icons.Outlined.CheckCircle, onClick = onAuditClick),
        AdminHubItem("Editar catálogo", "Planos, cores e configurações", Icons.Outlined.Edit, onClick = onEditCatalogClick),
    )

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 18.dp) {
        PremiumHeader(
            title = "Admin Planos",
            subtitle = "Menu dividido por página para reduzir leituras",
            icon = Icons.Outlined.WorkspacePremium,
            onBackClick = onBackClick,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Catálogo de planos",
                onClick = onEditCatalogClick,
                icon = Icons.Outlined.ReceiptLong,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Marketing CSS",
                onClick = onPublicPlansClick,
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f),
            )
        }
        HubSection(title = "Listas por plano", items = planListItems)
        HubSection(title = "Gestão e auditoria", items = managementItems)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminManagementHubScreen(
    onEventsClick: () -> Unit,
    onStoreClick: () -> Unit,
    onTrainingClick: () -> Unit,
    onFinanceClick: () -> Unit,
    onCommercialBiClick: () -> Unit,
    onOperationalBiClick: () -> Unit,
    onGateBiClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = listOf(
        AdminHubItem("Eventos", "BI comercial, operacional, portaria, estratégico e modo vendas.", Icons.Outlined.ReceiptLong, onClick = onEventsClick),
        AdminHubItem("BI Loja", "Produtos oficiais da atlética, sem misturar mini vendors ou entidades.", Icons.Outlined.ShoppingBag, onClick = onStoreClick),
        AdminHubItem("Treinos", "Presenças, confirmações, modalidades e frequência por data.", Icons.Outlined.FitnessCenter, onClick = onTrainingClick),
        AdminHubItem("Financeiro", "Extrato completo com pedidos, aprovações, QR, transferências, custo e CSV.", Icons.Outlined.BarChart, onClick = onFinanceClick),
    )
    val biItems = listOf(
        AdminHubItem("BI Comercial", "Integrações de BI", Icons.Outlined.BarChart, onClick = onCommercialBiClick),
        AdminHubItem("BI Operacional", "Integrações de BI", Icons.Outlined.BarChart, onClick = onOperationalBiClick),
        AdminHubItem("BI Portaria", "Integrações de BI", Icons.Outlined.BarChart, onClick = onGateBiClick),
        AdminHubItem("BI Loja", "Integrações de BI", Icons.Outlined.ShoppingBag, onClick = onStoreClick),
    )

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 18.dp) {
        PremiumHeader(
            title = "Gestão",
            subtitle = "Painéis de BI e extrato financeiro da atlética.",
            icon = Icons.Outlined.BarChart,
            onBackClick = onBackClick,
        )
        PremiumSecondaryButton(
            text = "Abrir Financeiro",
            onClick = onFinanceClick,
            icon = Icons.Outlined.BarChart,
        )
        HubSection(title = "Gestão administrativa", items = cards)
        HubSection(title = "Integrações de BI", items = biItems)
    }
}

@Composable
fun AdminPendingRouteScreen(
    title: String,
    source: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp) {
        PremiumHeader(
            title = title,
            subtitle = "Pendente de tradução a partir do web-reference",
            icon = Icons.Outlined.ReceiptLong,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Rota ainda não traduzida.",
            subtitle = "Fonte web: $source. Não há mock nem dado fake nesta tela.",
            icon = Icons.Outlined.ReceiptLong,
            accent = PremiumAmber,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HubSection(
    title: String,
    items: List<AdminHubItem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                AdminSimpleHubCard(item = item)
            }
        }
    }
}

@Composable
private fun AdminSimpleHubCard(item: AdminHubItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
        onClick = item.onClick,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = item.accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, item.accent.copy(alpha = 0.34f)),
            ) {
                Icon(item.icon, contentDescription = null, tint = item.accent, modifier = Modifier.padding(11.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(text = item.description, color = PremiumZinc400, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PremiumZinc500)
        }
    }
}
