package com.example.usc1.ui.vendor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun MiniVendorProductCard(
    product: MiniVendorProduct,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = product.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
                    .border(1.dp, PremiumZinc800, RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = product.stockLabel, color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                PremiumChip(label = product.status, icon = Icons.Outlined.Storefront, accent = PremiumBrand)
            }
            Text(text = product.priceLabel, color = PremiumBrand, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MiniVendorOrderCard(
    order: MiniVendorOrder,
    modifier: Modifier = Modifier,
) {
    val accent = if (order.status == MiniVendorOrderStatus.Pending) PremiumAmber else PremiumBrand
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
            ) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.padding(11.dp), tint = accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = order.id, color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = order.customerName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(text = "${order.productName} • ${order.createdAtLabel}", color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = order.amountLabel, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
                PremiumChip(label = order.status.label, icon = Icons.Outlined.CheckCircle, accent = accent)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
private fun MiniVendorProductCardPreview() {
    UscTheme(darkTheme = true) {
        MiniVendorProductCard(product = MiniVendorMockData.products.first(), modifier = Modifier.padding(16.dp))
    }
}
