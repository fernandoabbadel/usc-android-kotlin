package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminStoreReview

@Composable
fun AdminStoreReviewsScreen(
    state: AdminStoreReviewsUiState,
    onApproveClick: (AdminStoreReview) -> Unit,
    onRejectClick: (AdminStoreReview) -> Unit,
    onProductClick: (String) -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.reviews.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Reviews",
            subtitle = "Leitura dedicada: somente avaliacoes",
            icon = Icons.Outlined.StarRate,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )

        state.actionMessage?.let { message ->
            AdminStoreReviewsMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            AdminStoreReviewsMessage(message = message, color = Color(0xFFFCA5A5))
        }

        if (state.pagedReviews.isEmpty()) {
            PremiumEmptyState(
                title = "Sem reviews pendentes.",
                subtitle = "A leitura dedicada não retornou avaliações pendentes para este tenant.",
                icon = Icons.Outlined.StarRate,
                accent = PremiumAmber,
            )
        } else {
            state.pagedReviews.forEach { review ->
                AdminStoreReviewCard(
                    review = review,
                    isMutating = state.mutatingReviewId == review.id,
                    onApproveClick = { onApproveClick(review) },
                    onRejectClick = { onRejectClick(review) },
                    onProductClick = {
                        if (review.productId.isNotBlank()) {
                            onProductClick(review.productId)
                        }
                    },
                )
            }
        }

        if (state.pendingReviews.size > AdminStoreReviewsUiState.PageSize) {
            AdminStoreReviewsPager(
                page = state.page,
                totalPages = state.totalPages,
                onPreviousPageClick = onPreviousPageClick,
                onNextPageClick = onNextPageClick,
            )
        }

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
private fun AdminStoreReviewCard(
    review: AdminStoreReview,
    isMutating: Boolean,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onProductClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
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
                    text = review.userName.ifBlank { "Usuário" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = review.comment.ifBlank { "Sem comentario" },
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < review.rating) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (index < review.rating) PremiumAmber else PremiumZinc700,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        PremiumSecondaryButton(
            text = "Ver produto",
            onClick = onProductClick,
            enabled = review.productId.isNotBlank(),
            accent = PremiumZinc400,
            icon = Icons.Outlined.OpenInNew,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Aprovar",
                onClick = onApproveClick,
                enabled = !isMutating,
                accent = PremiumBrandAccent,
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Rejeitar",
                onClick = onRejectClick,
                enabled = !isMutating,
                accent = Color(0xFFF87171),
                icon = Icons.Outlined.Cancel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AdminStoreReviewsPager(
    page: Int,
    totalPages: Int,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Página $page de $totalPages",
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Anterior",
                onClick = onPreviousPageClick,
                enabled = page > 1,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Próxima",
                onClick = onNextPageClick,
                enabled = page < totalPages,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AdminStoreReviewsMessage(
    message: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
