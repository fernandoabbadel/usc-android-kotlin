package com.example.usc1.domain.repository

import com.example.usc1.domain.model.PlatformFaqConfig

interface PlatformFaqRepository {
    /** Mesma linha lida por `/api/public/faq`: `site_config` com id `faq_page`. */
    suspend fun getFaqConfig(): PlatformFaqConfig
}
