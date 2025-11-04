package com.katapandroid.lazybones.core.network.di

import com.katapandroid.lazybones.core.domain.service.TelegramGateway
import com.katapandroid.lazybones.core.network.TelegramGatewayImpl
import org.koin.dsl.module

val networkModule = module {
    single<TelegramGateway> { TelegramGatewayImpl() }
}
