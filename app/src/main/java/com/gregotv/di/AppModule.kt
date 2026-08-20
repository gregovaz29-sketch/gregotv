package com.gregotv.di

import android.content.Context
import androidx.room.Room
import com.gregotv.data.db.ALL_MIGRATIONS
import com.gregotv.data.db.AppDatabase
import com.gregotv.data.db.ChannelHealthDao
import com.gregotv.data.db.FavoriteDao
import com.gregotv.data.db.ProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        // No destructive fallback: favorites and progress must survive schema
        // bumps. Every version change must ship a Migration in Migrations.kt.
        Room.databaseBuilder(ctx, AppDatabase::class.java, "gregotv.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideChannelHealthDao(db: AppDatabase): ChannelHealthDao =
        db.channelHealthDao()
}
