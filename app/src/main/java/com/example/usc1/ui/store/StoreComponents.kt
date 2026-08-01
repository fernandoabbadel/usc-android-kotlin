package com.example.usc1.ui.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material.icons.outlined.ShoppingBag
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

@Composable
fun StoreProductImage(
    product: StoreProduct,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
) {
    val fallbackPainter = painterResource(id = product.imageRes)
    val imageUrl = product.imageUrl?.trim()?.takeIf { it.isNotBlank() }
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
        )
    } else {
        Image(
            painter = fallbackPainter,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha,
        )
    }
}

@Composable
fun StoreProductImageCard(
    product: StoreProduct,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 294.dp,
    accent: Color = productStatusColor(product.status),
    imageAlpha: Float = 0.78f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(30.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(30.dp)),
    ) {
        StoreProductImage(
            product = product,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = imageAlpha,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black,
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun ProductCard(
    product: StoreProduct,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = product.tagColor.toStoreColor(productStatusColor(product.status))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(226.dp)
                    .background(Color.Black),
            ) {
                StoreProductImage(
                    product = product,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.78f,
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
                        accent = accent,
                        filled = product.status == StoreProductStatus.Available,
                    )
                    ProductStatusChip(status = product.status)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    color = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
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
                StoreSellerLine(product = product)
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
                            letterSpacing = 1.sp,
                        )
                        product.oldPriceLabel?.let {
                            Text(
                                text = it,
                                color = PremiumZinc500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                        Text(
                            text = product.priceLabel,
                            color = productStatusColor(product.status),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    PremiumChip(label = product.category, accent = PremiumBrandAccent)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProductColorPreview(colors = product.colors)
                    Text(
                        text = "${product.likesCount} curtidas • ${product.soldCount} vendidos",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
fun StoreCategoryCard(
    category: StoreCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = category.buttonColor.toStoreColor(PremiumBrand)
    Box(
        modifier = modifier
            .width(154.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = if (selected) 0.86f else 0.28f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        val fallbackPainter = painterResource(id = com.example.usc1.R.drawable.logo_platform_web)
        if (!category.coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = category.coverImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                alpha = 0.74f,
            )
        } else {
            Image(
                painter = fallbackPainter,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(92.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.26f,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.16f),
                            Color.Black.copy(alpha = 0.56f),
                            Color.Black.copy(alpha = 0.94f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PremiumChip(label = category.name, accent = accent, filled = selected)
            PremiumChip(
                label = if (category.isReceivingOrders) "Ativo" else "Pausado",
                accent = if (category.isReceivingOrders) PremiumBrand else PremiumAmber,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color.Black,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
            ) {
                if (!category.sellerLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = category.sellerLogoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = fallbackPainter,
                        error = fallbackPainter,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp),
                        tint = accent,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = category.sellerName.ifBlank { category.sellerType.label },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category.sellerType.label.uppercase(),
                    color = accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StoreSellerLine(product: StoreProduct) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fallbackPainter = painterResource(id = product.imageRes)
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = Color.Black,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            if (!product.sellerLogoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.sellerLogoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = fallbackPainter,
                    error = fallbackPainter,
                )
            } else {
                Image(
                    painter = fallbackPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = product.sellerName.ifBlank { product.sellerType.label },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.sellerType.label.uppercase(),
                color = PremiumZinc500,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun ProductColorPreview(colors: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        val preview = colors.take(5).ifEmpty { listOf("#10B981") }
        preview.forEach { color ->
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = color.toStoreColor(PremiumZinc800),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            ) {}
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
            StoreProductImage(
                product = item.product,
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
                val optionSummary = listOf(item.variantLabel, item.colorLabel)
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .joinToString(" • ")
                if (optionSummary.isNotBlank()) {
                    Text(
                        text = optionSummary,
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
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
        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
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

private fun String?.toStoreColor(fallback: Color): Color {
    val clean = this?.trim().orEmpty()
    if (clean.isBlank()) return fallback
    return when (clean.lowercase()) {
        "emerald", "green", "verde" -> PremiumBrand
        "amber", "yellow", "amarelo", "laranja", "orange" -> PremiumAmber
        "purple", "violeta", "roxo" -> PremiumPurple
        "red", "vermelho" -> PremiumRed
        else -> runCatching {
            Color(android.graphics.Color.parseColor(if (clean.startsWith("#")) clean else "#$clean"))
        }.getOrDefault(fallback)
    }
}
