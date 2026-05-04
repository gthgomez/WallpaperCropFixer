package com.wallpapercropfixer.domain.usecase

import com.wallpapercropfixer.domain.model.SubjectAnalysis
import com.wallpapercropfixer.domain.repository.FaceDetectionRepository
import javax.inject.Inject

class AnalyzeSubjectUseCase @Inject constructor(
    private val faceDetectionRepository: FaceDetectionRepository
) {
    suspend operator fun invoke(uri: String): SubjectAnalysis =
        faceDetectionRepository.analyzeFaces(uri)
}
