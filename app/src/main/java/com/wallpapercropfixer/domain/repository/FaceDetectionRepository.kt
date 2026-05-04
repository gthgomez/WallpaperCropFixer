package com.wallpapercropfixer.domain.repository

import com.wallpapercropfixer.domain.model.SubjectAnalysis

interface FaceDetectionRepository {
    suspend fun analyzeFaces(uri: String): SubjectAnalysis
}
