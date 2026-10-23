package com.dan1eidtj.mayas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.dan1eidtj.data.BackendApi
import com.dan1eidtj.data.buyShopItemViaBackend
import com.dan1eidtj.data.refreshBalanceAndInventory
import com.dan1eidtj.data.BuyItemResult

class MonetizationVM(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val uid get() = auth.currentUser?.uid ?: ""

    private val purchasingItems = mutableSetOf<String>()

    fun buyItem(
        itemId: String,
        onLowBalance: () -> Unit,
        onSuccess: (newBalance: Int) -> Unit,
        onAlreadyOwned: () -> Unit = {},
        onOutOfSeason: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (uid.isEmpty()) {
            onError("Не авторизован")
            return
        }
        if (itemId.isBlank()) {
            onError("Некорректный идентификатор товара")
            return
        }
        if (!purchasingItems.add(itemId)) return

        viewModelScope.launch {
            try {
                when (val result = buyShopItemViaBackend(itemId)) {
                    is BuyItemResult.Success -> onSuccess(result.newBalance)
                    BuyItemResult.LowBalance -> onLowBalance()
                    BuyItemResult.AlreadyOwned -> onAlreadyOwned()
                    BuyItemResult.OutOfSeason -> onOutOfSeason()
                    BuyItemResult.ItemNotFound -> onError("Товар не найден")
                    is BuyItemResult.Error -> {
                        Log.e("MonetizationVM", "Ошибка покупки: ${result.message}")
                        if (!result.outcomeUnknown) {
                            onError(result.message)
                        } else {
                            val fresh = refreshBalanceAndInventory()
                            val balance = fresh?.balance
                            when {
                                fresh == null -> onError("Не удалось подтвердить покупку. Проверь баланс и инвентарь, прежде чем пробовать снова.")
                                itemId in fresh.ownedItems && balance != null -> onSuccess(balance.toInt())
                                itemId in fresh.ownedItems -> onAlreadyOwned()
                                else -> onError(result.message)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MonetizationVM", "Не удалось выполнить покупку $itemId", e)
                onError(e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Не удалось выполнить покупку. Проверь подключение и попробуй ещё раз.")
            } finally {
                purchasingItems.remove(itemId)
            }
        }
    }

    fun redeemPromoCode(code: String, onResult: (String) -> Unit) {
        if (uid.isEmpty()) {
            onResult("Не авторизован")
            return
        }
        val cleanCode = code.trim()
        if (cleanCode.isEmpty()) {
            onResult("Введите промокод")
            return
        }
        viewModelScope.launch {
            try {
                val result = BackendApi.redeemPromoCode(cleanCode)
                when (result.type) {
                    "balance" -> onResult("Промокод активирован: +${result.credited} монет")
                    "premium" -> onResult("Промокод успешно активирован!")
                    else -> onResult("Промокод активирован")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MonetizationVM", "Ошибка серверной активации промокода", e)
                val message = when (e.message?.substringBefore(' ')) {
                    "PROMO_NOT_FOUND" -> "Промокод не найден"
                    "PROMO_INACTIVE" -> "Промокод недействителен или истёк"
                    "PROMO_ALREADY_REDEEMED" -> "Этот промокод уже использован в аккаунте"
                    "INVALID_PROMO_TYPE", "INVALID_PROMO_VALUE" -> "Промокод настроен неверно"
                    else -> "Не удалось активировать промокод. Проверь подключение и попробуй ещё раз."
                }
                onResult(message)
            }
        }
    }
}