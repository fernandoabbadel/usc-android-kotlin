package com.example.usc1.ui.partners

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.PartnerCoupon
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerStatus
import com.example.usc1.domain.model.PartnerTier

@Composable
fun PartnerCard(
    partner: PartnerRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = partnerAccent(partner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.34f),
                                PremiumZinc900,
                            ),
                        ),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PremiumChip(label = partner.category, icon = Icons.Outlined.Storefront, accent = PremiumBrand, filled = true)
                    PartnerStatusChip(partner = partner)
                }
                Text(
                    text = partner.name.uppercase(),
                    color = Color.White,
                    fontSize = 28.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(42.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = Color.Black,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = partner.description.ifBlank { partner.category },
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = PremiumBrand)
                    Text(
                        text = partner.address.ifBlank { partner.businessHours.ifBlank { "-" } },
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun PartnerStatusChip(
    partner: PartnerRecord,
    modifier: Modifier = Modifier,
) {
    val icon = when {
        partner.tier == PartnerTier.Ouro && partner.status == PartnerStatus.Active -> Icons.Outlined.Star
        partner.status == PartnerStatus.Active -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.ReceiptLong
    }
    PremiumChip(
        label = partner.publicStatusLabel,
        icon = icon,
        accent = partnerAccent(partner),
        modifier = modifier,
    )
}

@Composable
fun PartnerBenefitCard(
    benefit: PartnerCoupon,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = benefit.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = benefit.rule.ifBlank { "Cupom ativo" },
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            PremiumChip(label = benefit.valueLabel.ifBlank { "-" }, accent = PremiumAmber, filled = true)
        }
    }
}

fun partnerAccent(partner: PartnerRecord): Color = when {
    partner.tier == PartnerTier.Ouro && partner.status == PartnerStatus.Active -> PremiumAmber
    partner.status == PartnerStatus.Disabled -> PremiumRed
    else -> PremiumBrand
}
