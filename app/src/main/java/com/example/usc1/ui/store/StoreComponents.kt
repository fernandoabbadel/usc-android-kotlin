package com.example.usc1.ui.store

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material.icons.outlined.ShoppingBag
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

@Composable
fun ProductCard(
    product: StoreProduct,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(PremiumZinc900)
            .border(1.dp, productStatusColor(product.status).copy(alpha = 0.24f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(214.dp)
                    .background(Color.Black),
            ) {
                Image(
                    painter = painterResource(id = product.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.76f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Transparent,
                                    PremiumZinc900,
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PremiumChip(
                        label = product.badge,
                        accent = productStatusColor(product.status),
                        filled = product.status == StoreProductStatus.Available,
                    )
                    PremiumChip(label = product.category, accent = PremiumBrandAccent)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                        tint = Color.Black,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PREÇO",
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = product.priceLabel,
                            color = productStatusColor(product.status),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    ProductStatusChip(status = product.status)
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = item.product.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.product.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.quantity} unidade(s) • ${item.product.stockLabel}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                PremiumChip(label = item.product.category, accent = PremiumBrand)
            }
            Text(
                text = item.product.priceLabel,
                color = PremiumBrandAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
fun StoreOrderCard(
    order: StoreOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCard(
        modifier = modifier.clickable(onClick = onClick),
        accent = storeOrderColor(order.status),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = order.id.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${order.createdAtLabel} • ${order.pickupLabel}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StoreOrderStatusChip(status = order.status)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = order.paymentStatus.label.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.amountLabel,
                    color = storeOrderColor(order.status),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Surface(
                modifier = Modifier.size(42.dp),
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
    }
}

@Composable
fun ProductStatusChip(
    status: StoreProductStatus,
    modifier: Modifier = Modifier,
) {
    val icon = when (status) {
        StoreProductStatus.Available -> Icons.Outlined.CheckCircle
        StoreProductStatus.SoldOut -> Icons.Outlined.RemoveShoppingCart
        StoreProductStatus.ComingSoon -> Icons.Outlined.Inventory2
    }
    PremiumChip(
        label = status.label,
        icon = icon,
        accent = productStatusColor(status),
        modifier = modifier,
    )
}

@Composable
fun StoreOrderStatusChip(
    status: StoreOrderStatus,
    modifier: Modifier = Modifier,
) {
    PremiumChip(
        label = status.label,
        icon = Icons.Outlined.ReceiptLong,
        accent = storeOrderColor(status),
        modifier = modifier,
    )
}

@Composable
fun StoreSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

fun productStatusColor(status: StoreProductStatus): Color = when (status) {
    StoreProductStatus.Available -> PremiumBrand
    StoreProductStatus.SoldOut -> PremiumRed
    StoreProductStatus.ComingSoon -> PremiumAmber
}

fun storeOrderColor(status: StoreOrderStatus): Color = when (status) {
    StoreOrderStatus.Pending -> PremiumAmber
    StoreOrderStatus.Approved -> PremiumBrand
    StoreOrderStatus.Cancelled -> PremiumRed
}
