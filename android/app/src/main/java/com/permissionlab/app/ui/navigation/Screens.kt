package com.permissionlab.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Camera

@Serializable
object Gallery

@Serializable
data class PhotoDetail(val uri: String)
