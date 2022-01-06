package com.teampym.onlineclothingshopapplication.presentation.client.others

sealed class OtherDialogFragmentEvent {
    data class ShowErrorMessage(val msg: String) : OtherDialogFragmentEvent()
    object NavigateBack : OtherDialogFragmentEvent()
}
