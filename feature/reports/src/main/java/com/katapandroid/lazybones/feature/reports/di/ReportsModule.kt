package com.katapandroid.lazybones.feature.reports.di

import com.katapandroid.lazybones.feature.reports.ReportsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val reportsModule = module {
    viewModel { ReportsViewModel(get(), get(), get()) }
}
