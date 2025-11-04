package com.katapandroid.lazybones.feature.plan.di

import com.katapandroid.lazybones.feature.plan.PlanViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val planModule = module {
    viewModel { PlanViewModel(get(), get()) }
}
