@file:OptIn(ExperimentalLayoutApi::class)

package com.example.usc1.ui.partners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerTier

@Composable
fun PartnersScreen(
    state: PartnerUiState,
    onPartnerClick: (PartnerRecord) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    onSearchChange: (String) -> Unit = {},
) {
    if (state.isLoading && state.partners.isEmpty()) {
        PremiumLoadingState(text = "Carregando parceiros...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Parceiros",
            subtitle = "Benefícios por plano",
            icon = Icons.Outlined.Groups,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            PremiumChip(label = "Carteirinha ativa", icon = Icons.Outlined.QrCodeScanner, accent = PremiumBrand, filled = true)
            Text(
                text = "BENEFÍCIOS DO CARDUME",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Use a carteirinha digital ou QR visual para validar descontos em empresas parceiras.",
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        PremiumTextField(
            value = state.search,
            onValueChange = onSearchChange,
            label = "Buscar parceiro...",
            leadingIcon = Icons.Outlined.Search,
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Seções por plano, como o TIER_SECTIONS do web.
        state.sections.forEach { section ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (section.tier) {
                            PartnerTier.Ouro -> Icons.Outlined.WorkspacePremium
                            PartnerTier.Prata -> Icons.Outlined.Shield
                            PartnerTier.Standard -> Icons.Outlined.Star
                        },
                        contentDescription = null,
                        tint = when (section.tier) {
                            PartnerTier.Ouro -> PremiumAmber
                            PartnerTier.Prata -> PremiumZinc400
                            PartnerTier.Standard -> PremiumBrand
                        },
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = section.title.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = "${section.subtitle} • ${section.partners.size}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (section.partners.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhum parceiro neste plano.",
                    subtitle = "Assim que a atlética publicar parceiros deste plano, eles aparecem aqui.",
                    icon = Icons.Outlined.Storefront,
                )
            } else {
                section.partners.forEach { partner ->
                    PartnerCard(
                        partner = partner,
                        onClick = { onPartnerClick(partner) },
                    )
                }
            }
        }
    }
}

@Composable
fun PartnerDetailScreen(
    state: PartnerDetailUiState,
    onBenefitsClick: (PartnerRecord) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partner = state.partner
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando parceiro...", modifier = modifier)
        return
    }
    if (partner == null) {
        PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = "Parceiro",
                subtitle = "Registro não encontrado",
                icon = Icons.Outlined.Storefront,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Parceiro não encontrado.",
                subtitle = state.errorMessage ?: "A consulta por id e tenant_id não retornou dados.",
                icon = Icons.Outlined.Storefront,
            )
        }
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = partner.name,
            subtitle = partner.category,
            icon = Icons.Outlined.Storefront,
            accent = partnerAccent(partner),
            onBackClick = onBackClick,
        )

        PremiumCard(accent = partnerAccent(partner)) {
            PartnerStatusChip(partner = partner)
            Text(
                text = partner.name.uppercase(),
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = partner.description.ifBlank { partner.category },
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumInfoRow(label = "Categoria", value = partner.category, accent = partnerAccent(partner))
            PremiumInfoRow(label = "Status", value = partner.publicStatusLabel, accent = partnerAccent(partner))
            if (partner.address.isNotBlank()) {
                PremiumInfoRow(label = "Endereço", value = partner.address, accent = partnerAccent(partner))
            }
            if (partner.businessHours.isNotBlank()) {
                PremiumInfoRow(label = "Horário", value = partner.businessHours, accent = partnerAccent(partner))
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                partner.coupons.forEach { benefit ->
                    PremiumChip(label = benefit.valueLabel.ifBlank { benefit.title }, accent = PremiumAmber, filled = true)
                }
            }
        }

        PremiumPrimaryButton(
            text = "Ver benefícios",
            onClick = { onBenefitsClick(partner) },
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = partnerAccent(partner),
            enabled = partner.coupons.isNotEmpty(),
        )
    }
}

@Composable
fun PartnerBenefitsScreen(
    state: PartnerDetailUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partner = state.partner
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando benefícios...", modifier = modifier)
        return
    }
    if (partner == null) {
        PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
            PremiumHeader(
                title = "Benefícios",
                subtitle = "Parceiro não encontrado",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Sem parceiro.",
                subtitle = state.errorMessage ?: "A consulta por id e tenant_id não retornou dados.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )
        }
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Benefícios",
            subtitle = partner.name,
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = partnerAccent(partner),
            onBackClick = onBackClick,
        )

        if (partner.coupons.isEmpty()) {
            PremiumEmptyState(
                title = "Sem benefícios",
                subtitle = "Nenhum cupom ativo foi retornado para esta empresa.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )
        } else {
            partner.coupons.filter { it.active }.forEach { benefit ->
                PartnerBenefitCard(benefit = benefit)
            }
        }
    }
}
