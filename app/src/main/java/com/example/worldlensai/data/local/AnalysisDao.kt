package com.example.worldlensai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.worldlensai.model.Analysis
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    fun getAllAnalysis(): Flow<List<Analysis>>

    @Insert
    suspend fun insertAnalysis(analysis: Analysis)
}