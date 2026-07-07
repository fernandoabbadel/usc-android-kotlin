@file:OptIn(ExperimentalLayoutApi::class)

package com.example.usc1.ui.partners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumImageCard
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.ui.theme.UscTheme

@Composable
fun PartnersScreen(
    state: PartnerUiState,
    onPartnerClick: (PartnerCompany) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Parceiros",
            subtitle = "Empresas, cupons e benefícios da USC",
            icon = Icons.Outlined.Groups,
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

        Text(
            text = "EMPRESAS PARCEIRAS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 2.dp),
        )
        state.partners.forEach { partner ->
            PartnerCard(
                partner = partner,
                onClick = { onPartnerClick(partner) },
            )
        }
    }
}

@Composable
fun PartnerDetailScreen(
    partner: PartnerCompany,
    onBenefitsClick: (PartnerCompany) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = partner.name,
            subtitle = partner.category,
            icon = Icons.Outlined.Storefront,
            accent = partnerStatusColor(partner.status),
            onBackClick = onBackClick,
        )

        PremiumImageCard(
            imageRes = partner.imageRes,
            height = 286.dp,
            accent = partnerStatusColor(partner.status),
            imageAlpha = 0.72f,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(androidx.compose.ui.Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PartnerStatusChip(status = partner.status)
                Text(
                    text = partner.name.uppercase(),
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = partner.addressLabel,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        PremiumCard(accent = partnerStatusColor(partner.status)) {
            Text(
                text = partner.description,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumInfoRow(label = "Categoria", value = partner.category, accent = partnerStatusColor(partner.status))
            PremiumInfoRow(label = "Status", value = partner.status.label, accent = partnerStatusColor(partner.status))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                partner.benefits.forEach { benefit ->
                    PremiumChip(label = benefit.valueLabel, accent = PremiumAmber, filled = true)
                }
            }
        }

        PremiumPrimaryButton(
            text = "Ver benefícios",
            onClick = { onBenefitsClick(partner) },
            icon = Icons.Outlined.ReceiptLong,
            accent = partnerStatusColor(partner.status),
        )
        PremiumSecondaryButton(
            text = "Validar QR futuro",
            onClick = {},
            icon = Icons.Outlined.QrCodeScanner,
        )
    }
}

@Composable
fun PartnerBenefitsScreen(
    partner: PartnerCompany,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Benefícios",
            subtitle = partner.name,
            icon = Icons.Outlined.ReceiptLong,
            accent = partnerStatusColor(partner.status),
            onBackClick = onBackClick,
        )

        partner.benefits.forEach { benefit ->
            PartnerBenefitCard(benefit = benefit)
        }

        Text(
            text = "HISTÓRICO",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 2.dp),
        )
        if (partner.history.isEmpty()) {
            PremiumEmptyState(
                title = "Sem histórico",
                subtitle = "Nenhum benefício usado nesta empresa ainda.",
                icon = Icons.Outlined.ReceiptLong,
            )
        } else {
            partner.history.forEach { entry ->
                PartnerHistoryCard(entry = entry)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun PartnersScreenPreview() {
    UscTheme(darkTheme = true) {
        PartnersScreen(
            state = PartnerUiState(),
            onPartnerClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun PartnerDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        PartnerDetailScreen(
            partner = PartnersMockData.partners.first(),
            onBenefitsClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun PartnerBenefitsScreenPreview() {
    UscTheme(darkTheme = true) {
        PartnerBenefitsScreen(
            partner = PartnersMockData.partners.first(),
            onBackClick = {},
        )
    }
}
