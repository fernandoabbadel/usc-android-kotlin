package com.example.usc1.ui.bi.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Os ícones que o web importa do `lucide-react` nos `KpiCard` das cinco visões (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 6771-7586.
 * Cada nome aqui é o do `lucide`; o valor é o equivalente do Material Icons já usado no app.
 */
internal object BiIcon {
    val DollarSign: ImageVector = Icons.Outlined.AttachMoney
    val TrendingUp: ImageVector = Icons.Outlined.TrendingUp
    val CheckCircle2: ImageVector = Icons.Outlined.CheckCircle
    val Users: ImageVector = Icons.Outlined.Group
    val Ticket: ImageVector = Icons.Outlined.ConfirmationNumber
    val Package: ImageVector = Icons.Outlined.Inventory2
    val ShoppingBag: ImageVector = Icons.Outlined.ShoppingBag
    val BarChart3: ImageVector = Icons.Outlined.BarChart
    val Clock3: ImageVector = Icons.Outlined.AccessTime
    val AlertTriangle: ImageVector = Icons.Outlined.WarningAmber
    val QrCode: ImageVector = Icons.Outlined.QrCode2
    val Target: ImageVector = Icons.Outlined.TrackChanges
}
