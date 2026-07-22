package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminPartnerScansPage
import com.example.usc1.domain.model.AdminPartnersBundle
import com.example.usc1.domain.model.AdminPartnersPage
import com.example.usc1.domain.model.AdminPartnersTierCounts
import com.example.usc1.domain.model.PartnerForm
import com.example.usc1.domain.model.PartnerPasswordReset
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerStatus

interface PartnersRepository {
    suspend fun getPublicPartners(forceRefresh: Boolean = false): List<PartnerRecord>
    suspend fun getPartnerById(partnerId: String, forceRefresh: Boolean = false): PartnerRecord?
    suspend fun getAdminPartnersPage(
        status: PartnerStatus?,
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false,
    ): AdminPartnersPage

    suspend fun getAdminPartnerScansPage(
        page: Int,
        pageSize: Int,
        forceRefresh: Boolean = false,
    ): AdminPartnerScansPage

    suspend fun getAdminPartnersBundle(forceRefresh: Boolean = false): AdminPartnersBundle
    suspend fun getAdminPartnersTierCounts(forceRefresh: Boolean = false): AdminPartnersTierCounts
    suspend fun setPartnerStatus(partnerId: String, status: PartnerStatus)
    suspend fun savePartner(form: PartnerForm): PartnerRecord
    suspend fun requestPasswordReset(partnerId: String): PartnerPasswordReset
}
