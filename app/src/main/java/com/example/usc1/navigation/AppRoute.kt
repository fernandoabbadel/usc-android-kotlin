package com.example.usc1.navigation

object AppRoute {
    const val Login = "login"
    const val Register = "register"
    const val WaitingApproval = "waiting_approval"
    const val InviteRequired = "invite_required"
    const val BannedUser = "banned_user"
    const val AccountSecurity = "account_security"
    const val Dashboard = "dashboard"
    const val Profile = "profile"
    const val Settings = "settings"
    const val MembershipCard = "membership-card"
    const val Events = "events"
    const val EventDetail = "event-detail/{eventId}"
    const val EventCheckout = "event-checkout/{eventId}"
    const val EventTickets = "event-tickets"
    const val EventTicketDetail = "event-ticket-detail/{ticketId}"
    const val EventOrders = "event-orders"
    const val EventOrderDetail = "event-order-detail/{orderId}"

    fun eventDetail(eventId: String) = "event-detail/$eventId"
    fun eventCheckout(eventId: String) = "event-checkout/$eventId"
    fun eventTicketDetail(ticketId: String) = "event-ticket-detail/$ticketId"
    fun eventOrderDetail(orderId: String) = "event-order-detail/$orderId"
}
