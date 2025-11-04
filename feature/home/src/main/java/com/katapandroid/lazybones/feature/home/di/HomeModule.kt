package com.katapandroid.lazybones.feature.home.di

import com.katapandroid.lazybones.feature.home.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { MainViewModel(get(), get()) }
}
