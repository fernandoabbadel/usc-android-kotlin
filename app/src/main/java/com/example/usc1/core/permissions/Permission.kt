package com.example.usc1.core.permissions

enum class Permission(val remoteKey: String) {
    ViewDashboard("view_dashboard"),
    ViewProfile("view_profile"),
    ViewStore("view_store"),
    ViewEvents("view_events"),
    ViewPlans("view_plans"),
    ViewMembershipCard("view_membership_card"),
    ViewTraining("view_training"),
    ViewCommunity("view_community"),
    ViewCollectives("view_collectives"),
    ManageMiniVendor("manage_mini_vendor"),
    UseScanner("use_scanner"),
    ManageTenant("manage_tenant"),
}
