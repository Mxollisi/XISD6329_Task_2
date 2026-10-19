package za.co.giantautoupholstery.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import za.co.giantautoupholstery.data.local.GiantDatabase
import za.co.giantautoupholstery.data.local.Migrations
import za.co.giantautoupholstery.data.repo.*
import za.co.giantautoupholstery.domain.*
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {
    @Provides @Singleton
    fun database(@ApplicationContext c: Context): GiantDatabase =
        Room.databaseBuilder(c, GiantDatabase::class.java, GiantDatabase.NAME)
            .addMigrations(*Migrations.ALL) // never fallbackToDestructiveMigration(): user data must not be discarded
            .build()

    @Provides @Singleton fun firebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun clock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun auth(i: FirebaseAuthRepository): AuthRepository
    @Binds abstract fun profiles(i: ProfileRepositoryImpl): ProfileRepository
    @Binds abstract fun services(i: ServiceRepositoryImpl): ServiceRepository
    @Binds abstract fun bookings(i: BookingRepositoryImpl): BookingRepository
    @Binds abstract fun notifications(i: NotificationRepositoryImpl): NotificationRepository
    @Binds abstract fun support(i: SupportRepositoryImpl): SupportRepository
    @Binds abstract fun settings(i: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun notifier(i: OsNotifier): LocalNotifier
}
