package com.example.usc1.data.mapper

import com.example.usc1.data.dto.PlaceholderDto

fun PlaceholderDto.requireId(): String = id.trim()
