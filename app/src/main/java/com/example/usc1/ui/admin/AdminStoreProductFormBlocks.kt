package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.StorePaymentRecipient

/**
 * Gerenciador de recebedores do produto, portado de
 * `web-reference/src/app/admin/loja/produtos/page.tsx` 1470-1520.
 *
 * A lista de escolha sai do documento de recebedores do tenant; o diretório de membros aprovados
 * é o que alimenta esse documento (`paymentRecipients.ts` 141-210).
 */
@Composable
internal fun ProductRecipientsBlock(
    state: AdminStoreProductsUiState,
    onToggleReceiversManagerClick: () -> Unit,
    onToggleRecipientClick: (String) -> Unit,
    onSaveRecipientDirectoryClick: (List<StorePaymentRecipient>) -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recebedores do Produto",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Quem aparece como recebedor no pedido. O primeiro vira o principal.",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
            }
            PremiumChip(
                label = "${state.form.paymentRecipientUserIds.size} escolhidos",
                accent = PremiumBrandAccent,
            )
        }

        if (state.paymentRecipients.isEmpty()) {
            // 1502: mesmo texto vazio do web.
            Text(
                text = "Nenhum recebedor de produto cadastrado.",
                color = PremiumZinc400,
                fontSize = 11.sp,
            )
        } else {
            state.paymentRecipients.forEach { recipient ->
                val selected = state.form.paymentRecipientUserIds.contains(recipient.userId)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipient.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOf(recipient.turma, recipient.phone)
                                .filter { it.isNotBlank() }
                                .joinToString(" • "),
                            color = PremiumZinc500,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PremiumSecondaryButton(
                        text = if (selected) "Remover" else "Escolher",
                        onClick = { onToggleRecipientClick(recipient.userId) },
                        enabled = !state.isSaving,
                    )
                }
            }
        }

        PremiumSecondaryButton(
            text = if (state.showReceiversManager) "Fechar diretório" else "Gerenciar recebedores",
            onClick = onToggleReceiversManagerClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.showReceiversManager) {
            if (state.recipientDirectory.isEmpty()) {
                Text(
                    text = "Nenhum membro aprovado disponível no diretório do tenant.",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                )
            } else {
                Text(
                    text = "Toque para incluir o membro no documento de recebedores do tenant.",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                )
                state.recipientDirectory.forEach { candidate ->
                    val alreadySaved = state.paymentRecipients.any { it.userId == candidate.userId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = candidate.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = candidate.turma,
                                color = PremiumZinc500,
                                fontSize = 11.sp,
                            )
                        }
                        PremiumSecondaryButton(
                            text = if (alreadySaved) "Retirar" else "Adicionar",
                            onClick = {
                                val next = if (alreadySaved) {
                                    state.paymentRecipients.filterNot { it.userId == candidate.userId }
                                } else {
                                    state.paymentRecipients + candidate
                                }
                                onSaveRecipientDirectoryClick(next)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preço e visibilidade por plano, portados de
 * `web-reference/src/app/admin/loja/produtos/page.tsx` 1600-1640.
 *
 * O web deixa isso num modal; no celular vira um bloco recolhível dentro do próprio formulário,
 * pela mesma razão que o M8.2 trocou os modais de gráfico por `AlertDialog` — o formulário já é
 * uma coluna longa e um segundo modal empilhado não cabe bem na tela.
 */
@Composable
internal fun ProductPlanScopeBlock(
    state: AdminStoreProductsUiState,
    onTogglePlanModalClick: () -> Unit,
    onPlanPriceChange: (String, String, String) -> Unit,
    onPlanVisibilityChange: (String, String, Boolean) -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Preço e Visibilidade por Plano",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                // 1612: texto literal do web.
                text = "Só preencha quem tiver preço especial. Em branco, o plano usa o preço geral do produto.",
                color = PremiumZinc500,
                fontSize = 11.sp,
            )
        }

        if (state.planCatalog.isEmpty()) {
            Text(
                text = "Nenhum plano cadastrado no tenant.",
                color = PremiumZinc400,
                fontSize = 11.sp,
            )
        } else {
            PremiumSecondaryButton(
                text = if (state.isPlanModalOpen) {
                    "Fechar planos"
                } else {
                    "Editar ${state.planCatalog.size} planos"
                },
                onClick = onTogglePlanModalClick,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.isPlanModalOpen) {
                state.form.planScopeRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PremiumTextField(
                            value = row.price,
                            onValueChange = { onPlanPriceChange(row.planId, row.planName, it) },
                            label = row.planName.ifBlank { row.planId },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        PremiumSecondaryButton(
                            // 273: só o `false` explícito esconde o produto do plano.
                            text = if (row.visible) "Visível" else "Oculto",
                            onClick = {
                                onPlanVisibilityChange(row.planId, row.planName, !row.visible)
                            },
                            enabled = !state.isSaving,
                        )
                    }
                }
            }
        }
    }
}
