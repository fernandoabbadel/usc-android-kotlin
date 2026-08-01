package com.example.usc1.ui.orders

import com.example.usc1.domain.model.UserOrder
import com.example.usc1.domain.model.UserOrderFinanceConfig
import com.example.usc1.domain.model.UserOrderPaymentConfig
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab
import org.junit.Assert.assertEquals
import org.junit.Test

/** Espelha os contadores, a paginação e a resolução de PIX do `PedidosByTypePage`. */
class UserOrdersUiStateTest {

    @Test
    fun `contadores separam aprovado pendente e negado`() {
        val state = UserOrdersUiState(
            orders = listOf(
                order("1", UserOrderStatus.Aprovado),
                order("2", UserOrderStatus.Pendente),
                order("3", UserOrderStatus.Pendente),
                order("4", UserOrderStatus.Rejeitado),
            ),
        )

        assertEquals(1, state.approvedCount)
        assertEquals(2, state.pendingCount)
        assertEquals(1, state.rejectedCount)
    }

    @Test
    fun `lista filtra pelo status selecionado e pagina de dez em dez`() {
        val orders = (1..23).map { order("p$it", UserOrderStatus.Pendente) } +
            order("aprovado", UserOrderStatus.Aprovado)
        val state = UserOrdersUiState(orders = orders, statusFilter = UserOrderStatus.Pendente)

        assertEquals(23, state.filteredOrders.size)
        assertEquals(3, state.totalPages)
        assertEquals(UserOrdersPageSize, state.paginatedOrders.size)
        assertEquals("p1", state.paginatedOrders.first().id)
        assertEquals("p11", state.copy(page = 2).paginatedOrders.first().id)
        assertEquals(3, state.copy(page = 3).paginatedOrders.size)
    }

    @Test
    fun `payment_config do pedido tem prioridade sobre o financeiro do tenant`() {
        val state = UserOrdersUiState(
            financeConfig = UserOrderFinanceConfig(
                pixKey = "tenant@pix",
                bank = "Banco Tenant",
                holder = "Titular Tenant",
                whatsapp = "5511999999999",
            ),
            tenantBrandLabel = "USC",
        )
        val order = order("1", UserOrderStatus.Pendente).copy(
            paymentConfig = UserOrderPaymentConfig(pixKey = "pedido@pix", bank = "Banco Pedido"),
        )

        val resolved = state.resolvePaymentConfig(order)

        assertEquals("pedido@pix", resolved.pixKey)
        assertEquals("Banco Pedido", resolved.bank)
        // Sem titular no pedido, cai para o financeiro do tenant.
        assertEquals("Titular Tenant", resolved.holder)
        assertEquals("5511999999999", resolved.whatsapp)
    }

    @Test
    fun `sem payment_config e sem financeiro o fallback usa a marca do tenant`() {
        val state = UserOrdersUiState(tenantBrandLabel = "USC")

        val resolved = state.resolvePaymentConfig(order("1", UserOrderStatus.Pendente))

        assertEquals("financeiro@atletica.com.br", resolved.pixKey)
        assertEquals("Banco da Atlética", resolved.bank)
        assertEquals("USC", resolved.holder)
    }

    @Test
    fun `recebedor sem nome cai para a marca e setor financeiro`() {
        val state = UserOrdersUiState(tenantBrandLabel = "USC")
        val order = order("1", UserOrderStatus.Pendente)

        assertEquals("USC", state.resolveRecipientName(order))
        assertEquals("Financeiro", state.resolveRecipientClass(order))

        val withRecipient = order.copy(
            paymentConfig = UserOrderPaymentConfig(recipientName = "Ana", recipientClass = "T3"),
        )
        assertEquals("Ana", state.resolveRecipientName(withRecipient))
        assertEquals("T3", state.resolveRecipientClass(withRecipient))
    }

    @Test
    fun `codigo curto do pedido usa os oito primeiros caracteres em maiusculo`() {
        assertEquals("ABC12345", order("abc12345-6789", UserOrderStatus.Pendente).shortCode)
    }

    private fun order(id: String, status: UserOrderStatus) = UserOrder(
        id = id,
        tab = UserOrderTab.Eventos,
        title = "Evento",
        subtitle = "1x Lote unico",
        amount = 100.0,
        status = status,
        createdAtMillis = 0L,
        createdAtLabel = "",
    )
}
