package za.co.giantautoupholstery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import za.co.giantautoupholstery.data.CatalogSeeder
import javax.inject.Inject

@HiltAndroidApp
class GiantApp : Application() {
    @Inject lateinit var seeder: CatalogSeeder

    override fun onCreate() {
        super.onCreate()
        // Idempotent: inserts the labelled demonstration catalogue + FAQs only when missing (INSERT IGNORE).
        CoroutineScope(Dispatchers.IO).launch { seeder.seed() }
    }
}
