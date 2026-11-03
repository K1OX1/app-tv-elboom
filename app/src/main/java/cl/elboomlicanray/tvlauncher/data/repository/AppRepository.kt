package cl.elboomlicanray.tvlauncher.data.repository

import androidx.compose.ui.graphics.Color
import cl.elboomlicanray.tvlauncher.data.helper.AppLauncherHelper
import cl.elboomlicanray.tvlauncher.domain.model.AppCategory
import cl.elboomlicanray.tvlauncher.domain.model.AppItem
import cl.elboomlicanray.tvlauncher.domain.model.QuickServiceItem
import cl.elboomlicanray.tvlauncher.domain.model.ServiceActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface AppRepository {
    fun getFeaturedStreamingApps(): Flow<List<AppItem>>
    fun getCabinServices(): Flow<List<QuickServiceItem>>
}

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val launcherHelper: AppLauncherHelper
) : AppRepository {

    override fun getFeaturedStreamingApps(): Flow<List<AppItem>> = flow {
        emit(cl.elboomlicanray.tvlauncher.config.StreamingAppsConfig.defaultStreamingApps)
    }.flowOn(Dispatchers.IO)

    override fun getCabinServices(): Flow<List<QuickServiceItem>> = flow {
        emit(cl.elboomlicanray.tvlauncher.config.HotelServicesConfig.defaultCabinServices)
    }.flowOn(Dispatchers.IO)
}
